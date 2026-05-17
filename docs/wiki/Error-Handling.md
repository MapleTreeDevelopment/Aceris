# Error Handling

> Aceris reports failures through typed runtime exceptions.

## Exception Matrix

| Exception | Meaning |
| --- | --- |
| `TomlParseException` | Invalid TOML input |
| `TomlConversionException` | Invalid Java type conversion |
| `TomlWriteException` | Unsupported serialization value |
| `TomlConfigException` | Config-store file or lifecycle failure |
| `TomlScriptExecutionException` | Script lookup, engine, or execution failure |

## Parse Errors

```java
try {
    Toml.parse(text, "app.toml");
} catch (TomlParseException exception) {
    System.err.println(exception.format());
}
```

`TomlParseException` carries source span information for diagnostics.

## Recommended Handling

| Situation | Recommended response |
| --- | --- |
| User-provided config | Show formatted parse diagnostics. |
| Application defaults | Fail fast during startup. |
| Conversion mismatch | Fix the Java model or TOML value shape. |
| Script engine failure | Treat as host application execution failure. |

## Navigation

[Home](Home) | [Parsing TOML](Parsing-TOML) | [Type Mapping](Type-Mapping)

