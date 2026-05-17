package dev.mapletree.aceris;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class ValueParser {
    private static final String DEC_INT = "[+-]?(?:0|[1-9](?:_?[0-9])*)";
    private static final String EXP_INT = "[+-]?[0-9](?:_?[0-9])*";
    private static final Pattern DECIMAL_INTEGER = Pattern.compile(DEC_INT);
    private static final Pattern HEX_INTEGER = Pattern.compile("0x[0-9A-Fa-f](?:_?[0-9A-Fa-f])*");
    private static final Pattern OCTAL_INTEGER = Pattern.compile("0o[0-7](?:_?[0-7])*");
    private static final Pattern BINARY_INTEGER = Pattern.compile("0b[01](?:_?[01])*");
    private static final Pattern FLOAT = Pattern.compile(DEC_INT + "(?:\\.[0-9](?:_?[0-9])*)?(?:[eE]" + EXP_INT + ")|" + DEC_INT + "\\.[0-9](?:_?[0-9])*");
    private final String input;
    private final SourceText source;
    private final int line;
    private final int baseColumn;
    private final TomlOptions options;
    private int index;

    private ValueParser(String input, SourceText source, int line, int baseColumn, TomlOptions options) {
        this.input = input;
        this.source = source;
        this.line = line;
        this.baseColumn = baseColumn;
        this.options = options == null ? TomlOptions.DEFAULT : options;
    }

    static TomlValueNode parse(String input, SourceText source, int line, int baseColumn, TomlOptions options) {
        ValueParser parser = new ValueParser(input, source, line, baseColumn, options);
        TomlValueNode value = parser.parseValue(0);
        parser.skipTrivia();
        if (!parser.eof() && parser.peek() != '#') {
            parser.fail("Unexpected trailing characters");
        }
        return value;
    }

    private TomlValueNode parseValue(int depth) {
        if (depth > options.maxValueNestingDepth()) {
            fail("Value nesting exceeds maximum depth of " + options.maxValueNestingDepth());
        }
        skipTrivia();
        if (eof()) {
            fail("Expected a value");
        }
        return switch (peek()) {
            case '"' -> new TomlValueNode.Scalar(parseBasicString());
            case '\'' -> new TomlValueNode.Scalar(parseLiteralString());
            case '[' -> parseArray(depth + 1);
            case '{' -> parseInlineTable(depth + 1);
            default -> parseScalar();
        };
    }

    private String parseBasicString() {
        if (startsWith("\"\"\"")) {
            return parseMultilineBasicString();
        }
        index++;
        StringBuilder value = new StringBuilder();
        while (!eof()) {
            char c = input.charAt(index++);
            if (c == '"') {
                return value.toString();
            }
            if (c == '\\') {
                value.append(parseEscape());
            } else if (c == '\n' || c == '\r') {
                fail("Newlines are not allowed in basic strings");
            } else {
                value.append(c);
            }
            checkStringLength(value);
        }
        fail("Unterminated basic string");
        return "";
    }

    private String parseMultilineBasicString() {
        index += 3;
        if (!eof() && peek() == '\n') {
            index++;
        }
        StringBuilder value = new StringBuilder();
        while (!eof()) {
            if (startsWith("\"\"\"")) {
                index += 3;
                return value.toString();
            }
            char c = input.charAt(index++);
            if (c == '\\') {
                if (!eof() && peek() == '\n') {
                    index++;
                    while (!eof() && Character.isWhitespace(peek())) {
                        index++;
                    }
                } else {
                    value.append(parseEscape());
                }
            } else {
                value.append(c);
            }
            checkStringLength(value);
        }
        fail("Unterminated multiline basic string");
        return "";
    }

    private String parseLiteralString() {
        if (startsWith("'''")) {
            return parseMultilineLiteralString();
        }
        index++;
        StringBuilder value = new StringBuilder();
        while (!eof()) {
            char c = input.charAt(index++);
            if (c == '\'') {
                return value.toString();
            }
            if (c == '\n' || c == '\r') {
                fail("Newlines are not allowed in literal strings");
            }
            value.append(c);
            checkStringLength(value);
        }
        fail("Unterminated literal string");
        return "";
    }

    private String parseMultilineLiteralString() {
        index += 3;
        if (!eof() && peek() == '\n') {
            index++;
        }
        int end = input.indexOf("'''", index);
        if (end < 0) {
            fail("Unterminated multiline literal string");
        }
        String value = input.substring(index, end);
        if (value.length() > options.maxStringCharacters()) {
            fail("String exceeds maximum length of " + options.maxStringCharacters() + " characters");
        }
        index = end + 3;
        return value;
    }

    private TomlValueNode parseArray(int depth) {
        expect('[');
        List<TomlValueNode> values = new ArrayList<>();
        skipTrivia();
        if (!eof() && peek() == ']') {
            index++;
            return new TomlValueNode.ArrayValue(values);
        }
        while (!eof()) {
            if (values.size() >= options.maxArrayItems()) {
                fail("Array exceeds maximum item count of " + options.maxArrayItems());
            }
            values.add(parseValue(depth));
            skipTrivia();
            if (!eof() && peek() == ',') {
                index++;
                skipTrivia();
                if (!eof() && peek() == ']') {
                    index++;
                    return new TomlValueNode.ArrayValue(values);
                }
                continue;
            }
            if (!eof() && peek() == ']') {
                index++;
                return new TomlValueNode.ArrayValue(values);
            }
            fail("Expected ',' or ']' in array");
        }
        fail("Unterminated array");
        return new TomlValueNode.ArrayValue(values);
    }

    private TomlValueNode parseInlineTable(int depth) {
        expect('{');
        List<TomlAst.KeyValue> entries = new ArrayList<>();
        skipTrivia();
        if (!eof() && peek() == '}') {
            index++;
            return new TomlValueNode.InlineTable(entries);
        }
        while (!eof()) {
            if (entries.size() >= options.maxInlineTableEntries()) {
                fail("Inline table exceeds maximum entry count of " + options.maxInlineTableEntries());
            }
            skipTrivia();
            int keyStart = index;
            String keyText = readUntil('=').trim();
            if (eof() || peek() != '=') {
                fail("Expected '=' in inline table");
            }
            List<String> path = KeyPathParser.parse(keyText, source, line, baseColumn + keyStart);
            index++;
            TomlValueNode value = parseValue(depth);
            entries.add(new TomlAst.KeyValue(path, value, new SourceSpan(line, baseColumn + keyStart, 1)));
            skipTrivia();
            if (!eof() && peek() == ',') {
                index++;
                continue;
            }
            if (!eof() && peek() == '}') {
                index++;
                return new TomlValueNode.InlineTable(entries);
            }
            fail("Expected ',' or '}' in inline table");
        }
        fail("Unterminated inline table");
        return new TomlValueNode.InlineTable(entries);
    }

    private TomlValueNode parseScalar() {
        int start = index;
        while (!eof()) {
            char c = peek();
            if (c == ',' || c == ']' || c == '}' || c == '#') {
                break;
            }
            if (Character.isWhitespace(c)) {
                int checkpoint = index;
                skipTrivia();
                if (eof() || peek() == ',' || peek() == ']' || peek() == '}' || peek() == '#') {
                    index = checkpoint;
                    break;
                }
                index = checkpoint;
            }
            index++;
        }
        String raw = input.substring(start, index).trim();
        if (raw.isEmpty()) {
            fail("Expected a value");
        }
        if (raw.length() > options.maxScalarCharacters()) {
            fail("Scalar exceeds maximum length of " + options.maxScalarCharacters() + " characters");
        }
        return new TomlValueNode.Scalar(parseScalarToken(raw, start));
    }

    private Object parseScalarToken(String raw, int start) {
        if (raw.equals("true")) {
            return true;
        }
        if (raw.equals("false")) {
            return false;
        }
        if (raw.matches("[+-]?(inf|nan)")) {
            if (raw.endsWith("nan")) {
                return Double.NaN;
            }
            return raw.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        if (HEX_INTEGER.matcher(raw).matches()) {
            return parseInteger(raw, 16, "0x");
        }
        if (OCTAL_INTEGER.matcher(raw).matches()) {
            return parseInteger(raw, 8, "0o");
        }
        if (BINARY_INTEGER.matcher(raw).matches()) {
            return parseInteger(raw, 2, "0b");
        }
        if (DECIMAL_INTEGER.matcher(raw).matches()) {
            return parseInteger(raw, 10, "");
        }
        if (FLOAT.matcher(raw).matches()) {
            try {
                return new BigDecimal(raw.replace("_", ""));
            } catch (NumberFormatException exception) {
                throw source.error("Invalid floating point value", new SourceSpan(line, baseColumn + start, raw.length()));
            }
        }
        try {
            if (raw.matches("\\d{4}-\\d{2}-\\d{2}[Tt ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?[Zz]|\\d{4}-\\d{2}-\\d{2}[Tt ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?[+-]\\d{2}:\\d{2}")) {
                String normalized = raw.replace(' ', 'T');
                if (normalized.endsWith("z")) {
                    normalized = normalized.substring(0, normalized.length() - 1) + "Z";
                }
                return OffsetDateTime.parse(normalized);
            }
            if (raw.matches("\\d{4}-\\d{2}-\\d{2}[Tt ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?")) {
                return LocalDateTime.parse(raw.replace(' ', 'T'));
            }
            if (raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(raw);
            }
            if (raw.matches("\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?")) {
                return LocalTime.parse(raw);
            }
        } catch (RuntimeException ignored) {
            fail("Invalid temporal value");
        }
        throw source.error("Unsupported or invalid scalar value '" + raw + "'", new SourceSpan(line, baseColumn + start, raw.length()));
    }

    private Object parseInteger(String raw, int radix, String prefix) {
        String normalized = raw.replace("_", "");
        boolean negative = normalized.startsWith("-");
        if (normalized.startsWith("+") || normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        if (!prefix.isEmpty()) {
            normalized = normalized.substring(prefix.length());
        }
        BigInteger value;
        try {
            value = new BigInteger(normalized, radix);
        } catch (NumberFormatException exception) {
            throw source.error("Invalid integer value", new SourceSpan(line, baseColumn + index, 1));
        }
        if (negative) {
            value = value.negate();
        }
        try {
            return value.longValueExact();
        } catch (ArithmeticException ignored) {
            throw source.error("Integer value is outside the supported 64-bit signed range", new SourceSpan(line, baseColumn + index, 1));
        }
    }

    private String parseEscape() {
        if (eof()) {
            fail("Unterminated escape sequence");
        }
        char c = input.charAt(index++);
        return switch (c) {
            case 'b' -> "\b";
            case 't' -> "\t";
            case 'n' -> "\n";
            case 'f' -> "\f";
            case 'r' -> "\r";
            case '"' -> "\"";
            case '\\' -> "\\";
            case 'u' -> parseCodePoint(4);
            case 'U' -> parseCodePoint(8);
            default -> throw source.error("Unsupported escape sequence \\" + c, new SourceSpan(line, baseColumn + index - 2, 1));
        };
    }

    private String parseCodePoint(int length) {
        if (index + length > input.length()) {
            fail("Invalid unicode escape");
        }
        String raw = input.substring(index, index + length);
        if (!raw.matches("[0-9A-Fa-f]{" + length + "}")) {
            fail("Invalid unicode escape");
        }
        index += length;
        int codePoint = Integer.parseUnsignedInt(raw, 16);
        if (!Character.isValidCodePoint(codePoint) || Character.isSurrogate((char) codePoint)) {
            fail("Invalid unicode code point");
        }
        return new String(Character.toChars(codePoint));
    }

    private void checkStringLength(StringBuilder value) {
        if (value.length() > options.maxStringCharacters()) {
            fail("String exceeds maximum length of " + options.maxStringCharacters() + " characters");
        }
    }

    private String readUntil(char marker) {
        int start = index;
        char quote = 0;
        int square = 0;
        int curly = 0;
        while (!eof()) {
            char c = peek();
            if (quote != 0) {
                index += c == '\\' && quote == '"' ? 2 : 1;
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                index++;
                continue;
            }
            if (c == '[') {
                square++;
            } else if (c == ']') {
                square--;
            } else if (c == '{') {
                curly++;
            } else if (c == '}') {
                curly--;
            } else if (square == 0 && curly == 0 && c == marker) {
                break;
            }
            index++;
        }
        return input.substring(start, index);
    }

    private void skipTrivia() {
        while (!eof()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                index++;
                continue;
            }
            if (c == '#') {
                while (!eof() && peek() != '\n') {
                    index++;
                }
                continue;
            }
            break;
        }
    }

    private boolean startsWith(String prefix) {
        return input.startsWith(prefix, index);
    }

    private void expect(char c) {
        if (eof() || peek() != c) {
            fail("Expected '" + c + "'");
        }
        index++;
    }

    private char peek() {
        return input.charAt(index);
    }

    private boolean eof() {
        return index >= input.length();
    }

    private void fail(String message) {
        throw source.error(message, new SourceSpan(line, baseColumn + index, 1));
    }
}
