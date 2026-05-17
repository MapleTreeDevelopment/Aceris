package dev.mapletree.aceris;

/**
 * Exception thrown for syntactic or semantic TOML failures.
 */
public final class TomlParseException extends RuntimeException {
    private final SourceSpan span;
    private final String lineText;

    TomlParseException(String message, SourceSpan span, String lineText) {
        super(message);
        this.span = span;
        this.lineText = lineText == null ? "" : lineText;
    }

    public SourceSpan span() {
        return span;
    }

    public String lineText() {
        return lineText;
    }

    public String format() {
        if (span == null) {
            return getMessage();
        }
        String pointer = " ".repeat(Math.max(0, span.column() - 1)) + "^";
        return getMessage() + " at " + span.line() + ":" + span.column()
            + System.lineSeparator() + lineText
            + System.lineSeparator() + pointer;
    }
}
