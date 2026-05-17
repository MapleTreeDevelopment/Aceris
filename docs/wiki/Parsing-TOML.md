# Parsing TOML

> `Toml` is the entry point for document parsing and AST parsing.

## Common Entry Points

| Method | Use case |
| --- | --- |
| `Toml.parse(text)` | Parse an in-memory string. |
| `Toml.parse(text, filename)` | Add filename context to diagnostics. |
| `Toml.parse(text, filename, options)` | Parse with explicit safety limits. |
| `Toml.parseAst(text)` | Access parser output for tooling. |

## Parse a Document

```java
TomlDocument document = Toml.parse(text);
```

## Parse with a Filename

```java
TomlDocument document = Toml.parse(text, "app.toml");
```

The filename is used in parse error messages.

## Parse with Options

```java
TomlOptions options = new TomlOptions(
    1024 * 1024,
    64,
    10_000,
    10_000,
    256 * 1024,
    64 * 1024
);

TomlDocument document = Toml.parse(text, "app.toml", options);
```

## AST Access

```java
TomlAst.Document ast = Toml.parseAst(text);
```

The AST is useful for tooling, validation, and diagnostics. Most application code should use `TomlDocument`.

## Navigation

[Home](Home) | [Type Mapping](Type-Mapping) | [Error Handling](Error-Handling)

