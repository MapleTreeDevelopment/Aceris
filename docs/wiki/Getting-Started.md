# Getting Started

> Parse TOML, read typed values, map a table, and write TOML back out.

## Requirements

| Requirement | Version |
| --- | --- |
| Java | 17 or newer |
| Build tool | Gradle Wrapper included in the repository |
| Runtime dependencies | None |

## Parse a Document

```java
import dev.mapletree.aceris.Toml;
import dev.mapletree.aceris.TomlDocument;

TomlDocument config = Toml.parse("""
    title = "Example"

    [server]
    host = "127.0.0.1"
    port = 8080
    enabled = true
    """);

String host = config.getString("server.host").orElseThrow();
int port = config.getInteger("server.port").orElse(80);
boolean enabled = config.getBoolean("server.enabled").orElse(false);
```

## Map a Table

```java
public record ServerConfig(String host, int port, boolean enabled) {
}

ServerConfig server = config.getAs("server", ServerConfig.class).orElseThrow();
```

## Write TOML

```java
String toml = TomlWriter.writeObject(new AppConfig());
```

## Verify the Project

```sh
./gradlew check
```

## Next Steps

- [Parsing TOML](Parsing-TOML)
- [Type Mapping](Type-Mapping)
- [Object Mapping](Object-Mapping)

