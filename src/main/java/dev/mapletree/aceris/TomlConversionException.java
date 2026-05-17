package dev.mapletree.aceris;

/**
 * Exception thrown when a parsed TOML value cannot be converted to a requested Java type.
 */
public final class TomlConversionException extends RuntimeException {
    TomlConversionException(String message) {
        super(message);
    }

    TomlConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
