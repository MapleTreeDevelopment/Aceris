# Parser Safety Limits

> `TomlOptions` bounds parser work for large or hostile inputs.

## Default Limit Categories

| Limit | Purpose |
| --- | --- |
| Document characters | Caps input size. |
| Value nesting depth | Prevents deeply recursive structures. |
| Array items | Bounds array growth. |
| Inline table entries | Bounds inline table growth. |
| String characters | Prevents oversized strings. |
| Scalar characters | Prevents oversized scalar tokens. |

## Custom Limits

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

Use tighter limits for untrusted input or environments with strict memory budgets.

## Navigation

[Home](Home) | [Parsing TOML](Parsing-TOML) | [Error Handling](Error-Handling)

