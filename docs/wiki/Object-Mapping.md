# Object Mapping

> Map TOML tables into Java records, beans, private fields, and immutable constructor classes.

## Supported Shapes

| Java shape | Supported |
| --- | --- |
| Records | Yes |
| Mutable beans | Yes |
| Private fields | Yes |
| Immutable constructor classes | Yes |
| Explicit key annotations | Yes |

## Record Mapping

```java
public record ServerConfig(String host, int port, Mode mode) {
}

public enum Mode {
    ALPHA,
    BETA
}

ServerConfig server = config.getAs("server", ServerConfig.class).orElseThrow();
```

## Key Matching

Key matching supports exact names plus snake_case and kebab-case keys mapped to camelCase Java members.

```java
public record Contact(
    @TomlKey("email address") String emailAddress,
    @TomlDefault("guest") String role
) {
}
```

## Ignored Fields

```java
public final class AppConfig {
    @TomlIgnore
    private String runtimeSecret;
}
```

## Annotation Summary

| Annotation | Purpose |
| --- | --- |
| `@TomlKey` | Maps a field or component to an explicit TOML key. |
| `@TomlSection` | Writes or reads a nested table section. |
| `@TomlDefault` | Supplies a string default for missing values. |
| `@TomlIgnore` | Excludes a member from mapping or writing. |

## Navigation

[Home](Home) | [Type Mapping](Type-Mapping) | [Writing TOML](Writing-TOML)

