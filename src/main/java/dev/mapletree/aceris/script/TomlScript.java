package dev.mapletree.aceris.script;

import java.util.Map;

/**
 * A script definition loaded from a TOML document.
 *
 * <p>Aceris never executes scripts implicitly. A host application must register an engine in
 * {@link TomlScriptRunner} and explicitly run selected scripts.</p>
 */
public record TomlScript(String name, String engine, String source, Map<String, Object> options) {
    public TomlScript {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("script name must not be null or blank");
        }
        if (engine == null || engine.isBlank()) {
            throw new IllegalArgumentException("script engine must not be null or blank");
        }
        if (source == null) {
            throw new IllegalArgumentException("script source must not be null");
        }
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
