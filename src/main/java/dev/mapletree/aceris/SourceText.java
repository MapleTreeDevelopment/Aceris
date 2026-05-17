package dev.mapletree.aceris;

import java.util.ArrayList;
import java.util.List;

final class SourceText {
    private final String text;
    private final String filename;
    private final List<String> lines;

    SourceText(String text, String filename) {
        this.text = text == null ? "" : text;
        this.filename = filename == null || filename.isBlank() ? "<input>" : filename;
        validateControlCharacters();
        this.lines = splitLines(this.text);
    }

    String text() {
        return text;
    }

    String filename() {
        return filename;
    }

    int lineCount() {
        return lines.size();
    }

    String line(int oneBasedLine) {
        if (oneBasedLine < 1 || oneBasedLine > lines.size()) {
            return "";
        }
        return lines.get(oneBasedLine - 1);
    }

    TomlParseException error(String message, SourceSpan span) {
        return new TomlParseException(message, span, line(span.line()));
    }

    private static List<String> splitLines(String text) {
        String[] raw = text.split("\\R", -1);
        List<String> result = new ArrayList<>(raw.length);
        for (String line : raw) {
            result.add(line);
        }
        return result;
    }

    private void validateControlCharacters() {
        int line = 1;
        int column = 1;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c == '\n') {
                line++;
                column = 1;
                continue;
            }
            if (c == '\r') {
                if (index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    continue;
                }
                throw new TomlParseException("Bare carriage returns are not permitted", new SourceSpan(line, column, 1), rawLine(line));
            }
            if (c != '\t' && ((c >= 0x00 && c <= 0x1F) || c == 0x7F)) {
                throw new TomlParseException("Control character U+" + String.format("%04X", (int) c) + " is not permitted", new SourceSpan(line, column, 1), rawLine(line));
            }
            column++;
        }
    }

    private String rawLine(int oneBasedLine) {
        String[] raw = text.split("\\R", -1);
        if (oneBasedLine < 1 || oneBasedLine > raw.length) {
            return "";
        }
        return raw[oneBasedLine - 1];
    }
}
