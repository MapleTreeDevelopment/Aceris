package dev.mapletree.aceris;

public final class TomlWriteException extends RuntimeException {
    TomlWriteException(String message) {
        super(message);
    }

    TomlWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
