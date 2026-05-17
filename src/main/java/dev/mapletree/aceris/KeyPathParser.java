package dev.mapletree.aceris;

import java.util.ArrayList;
import java.util.List;

final class KeyPathParser {
    private final String input;
    private final SourceText source;
    private final int line;
    private final int baseColumn;
    private int index;
    private boolean lastKeyWasQuoted;

    private KeyPathParser(String input, SourceText source, int line, int baseColumn) {
        this.input = input;
        this.source = source;
        this.line = line;
        this.baseColumn = baseColumn;
    }

    static List<String> parse(String input, SourceText source, int line, int baseColumn) {
        return new KeyPathParser(input, source, line, baseColumn).parse();
    }

    private List<String> parse() {
        List<String> path = new ArrayList<>();
        while (index < input.length()) {
            skipWhitespace();
            int start = index;
            String key = readKey();
            if (key.isEmpty() && !lastKeyWasQuoted) {
                fail("Expected a key segment", start);
            }
            path.add(key);
            skipWhitespace();
            if (index >= input.length()) {
                break;
            }
            if (input.charAt(index) != '.') {
                fail("Expected '.' between dotted key segments", index);
            }
            index++;
        }
        if (path.isEmpty()) {
            fail("Expected a key", 0);
        }
        return path;
    }

    private String readKey() {
        lastKeyWasQuoted = false;
        if (peek('"')) {
            lastKeyWasQuoted = true;
            return readBasicString();
        }
        if (peek('\'')) {
            lastKeyWasQuoted = true;
            return readLiteralString();
        }
        StringBuilder value = new StringBuilder();
        while (index < input.length()) {
            char c = input.charAt(index);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                value.append(c);
                index++;
            } else {
                break;
            }
        }
        return value.toString();
    }

    private String readLiteralString() {
        index++;
        StringBuilder value = new StringBuilder();
        while (index < input.length()) {
            char c = input.charAt(index++);
            if (c == '\'') {
                return value.toString();
            }
            value.append(c);
        }
        fail("Unterminated literal key", input.length());
        return "";
    }

    private String readBasicString() {
        index++;
        StringBuilder value = new StringBuilder();
        while (index < input.length()) {
            char c = input.charAt(index++);
            if (c == '"') {
                return value.toString();
            }
            if (c == '\\') {
                value.append(readEscape());
            } else {
                value.append(c);
            }
        }
        fail("Unterminated quoted key", input.length());
        return "";
    }

    private String readEscape() {
        if (index >= input.length()) {
            fail("Unterminated escape sequence", index);
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
            case 'u' -> readCodePoint(4);
            case 'U' -> readCodePoint(8);
            default -> throw error("Unsupported escape sequence \\" + c, index - 2);
        };
    }

    private String readCodePoint(int length) {
        if (index + length > input.length()) {
            fail("Invalid unicode escape", index);
        }
        String raw = input.substring(index, index + length);
        if (!raw.matches("[0-9A-Fa-f]{" + length + "}")) {
            fail("Invalid unicode escape", index);
        }
        index += length;
        int codePoint = Integer.parseUnsignedInt(raw, 16);
        if (!Character.isValidCodePoint(codePoint) || Character.isSurrogate((char) codePoint)) {
            fail("Invalid unicode code point", index - length);
        }
        return new String(Character.toChars(codePoint));
    }

    private void skipWhitespace() {
        while (index < input.length() && (input.charAt(index) == ' ' || input.charAt(index) == '\t')) {
            index++;
        }
    }

    private boolean peek(char c) {
        return index < input.length() && input.charAt(index) == c;
    }

    private void fail(String message, int position) {
        throw error(message, position);
    }

    private TomlParseException error(String message, int position) {
        return source.error(message, new SourceSpan(line, baseColumn + position, 1));
    }
}
