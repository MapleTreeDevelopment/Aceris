package dev.mapletree.aceris.script;

import java.util.Map;

@FunctionalInterface
public interface TomlScriptEngine {
    Object execute(TomlScript script, Map<String, Object> context);
}
