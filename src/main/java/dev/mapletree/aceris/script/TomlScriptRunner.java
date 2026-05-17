package dev.mapletree.aceris.script;

import dev.mapletree.aceris.TomlDocument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Explicit script runner for script definitions stored in TOML.
 *
 * <p>The supported TOML shape is:</p>
 *
 * <pre>{@code
 * [scripts.hello]
 * engine = "echo"
 * source = "hello"
 * options.cwd = "/tmp"
 * }</pre>
 */
public final class TomlScriptRunner {
    private final Map<String, TomlScriptEngine> engines = new LinkedHashMap<>();

    public TomlScriptRunner register(String engineName, TomlScriptEngine engine) {
        if (engineName == null || engineName.isBlank()) {
            throw new IllegalArgumentException("engineName must not be null or blank");
        }
        if (engine == null) {
            throw new IllegalArgumentException("engine must not be null");
        }
        engines.put(engineName, engine);
        return this;
    }

    public List<TomlScript> scripts(TomlDocument document) {
        Map<String, Object> scripts = document.getTable("scripts").orElse(Map.of());
        return scripts.entrySet().stream()
            .filter(entry -> entry.getValue() instanceof Map<?, ?>)
            .map(entry -> scriptFromTable(entry.getKey(), (Map<?, ?>) entry.getValue()))
            .toList();
    }

    public Optional<TomlScript> script(TomlDocument document, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("script name must not be null or blank");
        }
        return scripts(document).stream()
            .filter(script -> script.name().equals(name))
            .findFirst();
    }

    public Object execute(TomlDocument document, String name) {
        return execute(document, name, Map.of());
    }

    public Object execute(TomlDocument document, String name, Map<String, Object> context) {
        TomlScript script = script(document, name)
            .orElseThrow(() -> new TomlScriptExecutionException("No script named '" + name + "'"));
        TomlScriptEngine engine = engines.get(script.engine());
        if (engine == null) {
            throw new TomlScriptExecutionException("No engine registered for '" + script.engine() + "'");
        }
        try {
            return engine.execute(script, context == null ? Map.of() : Map.copyOf(context));
        } catch (TomlScriptExecutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TomlScriptExecutionException("Script '" + name + "' failed", exception);
        }
    }

    public Map<String, Object> executeAll(TomlDocument document) {
        Map<String, Object> results = new LinkedHashMap<>();
        for (TomlScript script : scripts(document)) {
            results.put(script.name(), execute(document, script.name()));
        }
        return Map.copyOf(results);
    }

    @SuppressWarnings("unchecked")
    private TomlScript scriptFromTable(String name, Map<?, ?> table) {
        Object engine = table.get("engine");
        Object source = table.get("source");
        if (!(engine instanceof String engineName)) {
            throw new TomlScriptExecutionException("Script '" + name + "' must define string key 'engine'");
        }
        if (!(source instanceof String sourceText)) {
            throw new TomlScriptExecutionException("Script '" + name + "' must define string key 'source'");
        }
        Object options = table.get("options");
        Map<String, Object> optionMap = options instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        return new TomlScript(name, engineName, sourceText, optionMap);
    }
}
