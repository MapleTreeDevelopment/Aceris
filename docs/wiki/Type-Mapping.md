# Type Mapping

> `TomlDocument` exposes typed getters and conversion helpers for common JVM targets.

## Mapping Matrix

| TOML value | Java/Kotlin target |
| --- | --- |
| Integer | `byte`, `Byte`, `short`, `Short`, `int`, `Integer`, `long`, `Long`, `BigInteger` |
| Float | `float`, `Float`, `double`, `Double`, `BigDecimal` |
| String | `String`, `enum`, `URI`, `URL`, `Duration` |
| One-letter string | `char`, `Character` |
| Boolean | `boolean`, `Boolean` |
| Offset date-time | `OffsetDateTime`, `Instant`, `Date`, `Timestamp` |
| Local date-time | `LocalDateTime` |
| Local date | `LocalDate`, `java.sql.Date` |
| Local time | `LocalTime` |
| Array | `List<T>`, `Set<T>`, `SortedSet<T>`, `EnumSet<E>`, `T[]`, primitive arrays |
| Table | `Map<String, Object>`, records, beans, immutable constructor classes |

## Direct Getters

```java
int port = config.getInteger("server.port").orElseThrow();
URI uri = config.getUri("server.uri").orElseThrow();
List<Integer> ports = config.getList("server.ports", Integer.class).orElseThrow();
```

## Generic Types

Use `TomlType<T>` when `Class<T>` is not enough for nested generic types.

```java
List<ServerConfig> servers = config
    .getAs("servers", new TomlType<List<ServerConfig>>() {})
    .orElseThrow();
```

## Conversion Failures

Invalid conversions throw `TomlConversionException`.

## Navigation

[Home](Home) | [Object Mapping](Object-Mapping) | [Error Handling](Error-Handling)

