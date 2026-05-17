/**
 * Explicit host-controlled script integration for TOML-defined script metadata.
 *
 * <p>Aceris never executes script definitions while parsing. Host applications decide which
 * engines exist by registering {@link dev.mapletree.aceris.script.TomlScriptEngine} instances
 * with {@link dev.mapletree.aceris.script.TomlScriptRunner}.
 */
package dev.mapletree.aceris.script;
