package dev.mapletree.aceris.script;

public final class TomlScriptExecutionException extends RuntimeException {
    public TomlScriptExecutionException(String message) {
        super(message);
    }

    public TomlScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
