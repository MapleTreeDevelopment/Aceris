# Writing TOML

> `TomlWriter` serializes maps and Java objects into fresh TOML text.

## Write a Map

```java
String toml = TomlWriter.write(Map.of(
    "title", "Example",
    "server", Map.of(
        "host", "127.0.0.1",
        "port", 8080
    )
));
```

## Write an Object

```java
public final class Config {
    @TomlSection("XY")
    private XY xy = new XY();

    public static final class XY {
        private int test = 0;
    }
}

String toml = TomlWriter.writeObject(new Config());
```

Output:

```toml
[XY]
test = 0
```

## Supported Values

| Category | Examples |
| --- | --- |
| Scalars | strings, booleans, numbers, enums |
| Temporal values | dates, times, instants |
| Collections | arrays, lists, sets |
| Tables | maps, records, simple objects |

## Formatting Model

`TomlWriter` generates fresh TOML from values. It does not preserve comments or original formatting.

## Navigation

[Home](Home) | [Object Mapping](Object-Mapping) | [Global Config Store](Global-Config-Store)

