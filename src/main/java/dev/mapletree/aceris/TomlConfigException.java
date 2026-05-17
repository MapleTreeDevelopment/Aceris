package dev.mapletree.aceris;

public final class TomlConfigException extends RuntimeException {
    TomlConfigException(String message) {
        super(message);
    }

    TomlConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
