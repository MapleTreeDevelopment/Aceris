package dev.mapletree.aceris;

import java.util.ArrayList;
import java.util.List;

final class TomlParser {
    private final SourceText source;
    private final TomlOptions options;

    private TomlParser(SourceText source, TomlOptions options) {
        this.source = source;
        this.options = options == null ? TomlOptions.DEFAULT : options;
    }

    static TomlAst.Document parse(String text, String filename, TomlOptions options) {
        SourceText source = new SourceText(text, filename);
        TomlOptions resolvedOptions = options == null ? TomlOptions.DEFAULT : options;
        if (source.text().length() > resolvedOptions.maxDocumentCharacters()) {
            throw source.error("Document exceeds maximum size of " + resolvedOptions.maxDocumentCharacters() + " characters", new SourceSpan(1, 1, 1));
        }
        return new TomlParser(source, resolvedOptions).parse();
    }

    private TomlAst.Document parse() {
        List<TomlAst.Statement> statements = new ArrayList<>();

        for (int lineNumber = 1; lineNumber <= source.lineCount(); lineNumber++) {
            String line = source.line(lineNumber);
            String trimmed = line.trim();
            int leading = line.length() - line.stripLeading().length();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("[[")) {
                int close = trimmed.indexOf("]]", 2);
                if (close < 0) {
                    throw source.error("Unterminated array table header", new SourceSpan(lineNumber, leading + 1, 1));
                }
                String body = trimmed.substring(2, close).trim();
                ensureCommentOnly(trimmed.substring(close + 2), lineNumber, leading + close + 3);
                statements.add(new TomlAst.ArrayTable(
                    KeyPathParser.parse(body, source, lineNumber, leading + 3),
                    new SourceSpan(lineNumber, leading + 1, 1)
                ));
                continue;
            }

            if (trimmed.startsWith("[")) {
                int close = trimmed.indexOf(']', 1);
                if (close < 0) {
                    throw source.error("Unterminated table header", new SourceSpan(lineNumber, leading + 1, 1));
                }
                String body = trimmed.substring(1, close).trim();
                ensureCommentOnly(trimmed.substring(close + 1), lineNumber, leading + close + 2);
                statements.add(new TomlAst.Table(
                    KeyPathParser.parse(body, source, lineNumber, leading + 2),
                    new SourceSpan(lineNumber, leading + 1, 1)
                ));
                continue;
            }

            int equals = findTopLevelEquals(line);
            if (equals < 0) {
                throw source.error("Expected key/value assignment", new SourceSpan(lineNumber, leading + 1, 1));
            }

            String keyText = line.substring(0, equals).trim();
            int keyColumn = line.indexOf(keyText) + 1;
            ValueBlock block = collectValueText(lineNumber, equals + 1);
            TomlValueNode value = ValueParser.parse(block.text(), source, lineNumber, equals + 2, options);

            statements.add(new TomlAst.KeyValue(
                KeyPathParser.parse(keyText, source, lineNumber, keyColumn),
                value,
                new SourceSpan(lineNumber, keyColumn, 1)
            ));
            lineNumber = block.endLine();
        }

        return new TomlAst.Document(statements);
    }

    private void ensureCommentOnly(String text, int line, int column) {
        String trimmed = text.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
            throw source.error("Unexpected content after table header", new SourceSpan(line, column, 1));
        }
    }

    private int findTopLevelEquals(String line) {
        char quote = 0;
        for (int index = 0; index < line.length(); index++) {
            char c = line.charAt(index);
            if (quote != 0) {
                if (c == '\\' && quote == '"') {
                    index++;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                continue;
            }
            if (c == '=') {
                return index;
            }
        }
        return -1;
    }

    private ValueBlock collectValueText(int startLine, int valueStartIndex) {
        StringBuilder text = new StringBuilder(source.line(startLine).substring(valueStartIndex));
        int endLine = startLine;
        while (needsMoreValueText(text.toString()) && endLine < source.lineCount()) {
            endLine++;
            text.append('\n').append(source.line(endLine));
        }
        return new ValueBlock(text.toString(), endLine);
    }

    private boolean needsMoreValueText(String text) {
        char quote = 0;
        boolean triple = false;
        int square = 0;
        int curly = 0;

        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (quote != 0) {
                if (triple && index + 2 < text.length()
                    && text.charAt(index) == quote
                    && text.charAt(index + 1) == quote
                    && text.charAt(index + 2) == quote) {
                    index += 2;
                    quote = 0;
                    triple = false;
                } else if (!triple && c == '\\' && quote == '"') {
                    index++;
                } else if (!triple && c == quote) {
                    quote = 0;
                }
                continue;
            }

            if (index + 2 < text.length()
                && (text.startsWith("\"\"\"", index) || text.startsWith("'''", index))) {
                quote = c;
                triple = true;
                index += 2;
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                continue;
            }
            if (c == '#') {
                while (index < text.length() && text.charAt(index) != '\n') {
                    index++;
                }
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
            }
        }

        return quote != 0 || square > 0 || curly > 0;
    }

    private record ValueBlock(String text, int endLine) {
    }
}
