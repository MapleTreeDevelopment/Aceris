package dev.mapletree.aceris;

/**
 * Entry point for parsing TOML text.
 */
public final class Toml {
    private Toml() {
    }

    public static TomlDocument parse(String text) {
        return parse(text, "<input>");
    }

    public static TomlDocument parse(String text, String filename) {
        return parse(text, filename, TomlOptions.DEFAULT);
    }

    public static TomlDocument parse(String text, TomlOptions options) {
        return parse(text, "<input>", options);
    }

    public static TomlDocument parse(String text, String filename, TomlOptions options) {
        TomlAst.Document ast = parseAst(text, filename, options);
        return TomlInterpreter.interpret(ast, text, filename);
    }

    public static TomlAst.Document parseAst(String text) {
        return parseAst(text, "<input>");
    }

    public static TomlAst.Document parseAst(String text, String filename) {
        return parseAst(text, filename, TomlOptions.DEFAULT);
    }

    public static TomlAst.Document parseAst(String text, TomlOptions options) {
        return parseAst(text, "<input>", options);
    }

    public static TomlAst.Document parseAst(String text, String filename, TomlOptions options) {
        return TomlParser.parse(text, filename, options);
    }
}
