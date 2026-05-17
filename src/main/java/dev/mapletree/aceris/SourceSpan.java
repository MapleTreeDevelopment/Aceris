package dev.mapletree.aceris;

/**
 * One-based source location for diagnostics.
 */
public record SourceSpan(int line, int column, int length) {
    public SourceSpan {
        if (line < 1) {
            throw new IllegalArgumentException("line must be >= 1");
        }
        if (column < 1) {
            throw new IllegalArgumentException("column must be >= 1");
        }
        if (length < 1) {
            throw new IllegalArgumentException("length must be >= 1");
        }
    }
}
