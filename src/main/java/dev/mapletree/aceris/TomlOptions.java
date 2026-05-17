package dev.mapletree.aceris;

/**
 * Parser safety limits. Defaults are intentionally generous for normal config files, while still
 * preventing accidental or hostile inputs from consuming unbounded memory or stack.
 */
public record TomlOptions(
    int maxDocumentCharacters,
    int maxValueNestingDepth,
    int maxArrayItems,
    int maxInlineTableEntries,
    int maxStringCharacters,
    int maxScalarCharacters
) {
    public static final TomlOptions DEFAULT = new TomlOptions(
        16 * 1024 * 1024,
        128,
        100_000,
        100_000,
        8 * 1024 * 1024,
        1_000_000
    );

    public TomlOptions {
        requirePositive(maxDocumentCharacters, "maxDocumentCharacters");
        requirePositive(maxValueNestingDepth, "maxValueNestingDepth");
        requirePositive(maxArrayItems, "maxArrayItems");
        requirePositive(maxInlineTableEntries, "maxInlineTableEntries");
        requirePositive(maxStringCharacters, "maxStringCharacters");
        requirePositive(maxScalarCharacters, "maxScalarCharacters");
    }

    private static void requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be >= 1");
        }
    }
}
