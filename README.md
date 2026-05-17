<div align="center">

# Aceris

**TOML Toolkit for Java & Kotlin**

</div>

<p align="center">
  <a href="https://github.com/MapleTreeDevelopment/aceris/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/MapleTreeDevelopment/aceris/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-2f7d32">
  <img alt="TOML 1.0" src="https://img.shields.io/badge/TOML-1.0-f2b705">
  <img alt="Dependency free" src="https://img.shields.io/badge/runtime-dependency--free-1f6f43">
  <img alt="License MIT" src="https://img.shields.io/badge/license-MIT-4b5563">
</p>

Aceris provides a strict TOML parser, typed document API, object mapper, writer, file-backed config store, explicit script integration, conformance fixtures, crash tests, and lightweight benchmarks for JVM applications.

It is designed for application and library code that wants predictable configuration behavior without pulling in a large runtime dependency graph.

<table>
  <tr>
    <th align="left">Latest Stable Build</th>
    <td><strong>Not published yet</strong></td>
  </tr>
  <tr>
    <th align="left">Current Development Version</th>
    <td><code>0.1.0-SNAPSHOT</code></td>
  </tr>
  <tr>
    <th align="left">Planned Maven Coordinate</th>
    <td><code>dev.mapletree:aceris:0.1.0</code></td>
  </tr>
  <tr>
    <th align="left">Java Baseline</th>
    <td>Java 17</td>
  </tr>
  <tr>
    <th align="left">Build Command</th>
    <td><code>./gradlew check</code></td>
  </tr>
</table>

## Contents

- [Status](#status)
- [Highlights](#highlights)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [API Guide](#api-guide)
- [Quality](#quality)
- [Project Information](#project-information)
- [License](#license)

## Status

Aceris is under active development and currently targets TOML 1.0 behavior. The implementation already covers common production configuration scenarios, selected official `toml-lang/toml-test` fixtures, and many invalid-input stability cases.

The conformance suite is intentionally incremental: `TomlConformanceTest` runs a selected green TOML 1.0 fixture set today, and the fixture set can be expanded as parser coverage grows.

## Highlights

- Dependency-free JVM core
- Parser and interpreter layers with AST access
- Typed getters for primitives, numbers, strings, collections, temporal values, URLs, URIs, and enums
- Object mapping for records, beans, private fields, and immutable constructor classes
- `@TomlKey`, `@TomlSection`, `@TomlDefault`, and `@TomlIgnore`
- Writer/serializer for maps and Java objects
- Global config store for file-backed application settings
- Explicit script runner with registered host engines
- Parser safety limits for document size, nesting, arrays, strings, and scalars
- Gradle Wrapper, CI workflow, test suite, crash test, conformance fixtures, and dependency-free benchmark

## Installation

Aceris is currently distributed from source and GitHub release artifacts until the first Maven Central release is available. The planned first public coordinate is:

```kotlin
implementation("dev.mapletree:aceris:0.1.0")
```

Build locally:

```sh
./gradlew build
```

For development and experiments, include the project as a Gradle composite build or publish it to a local Maven repository.

## Quick Start

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

<details>
<summary>Kotlin quick start</summary>

```kotlin
import dev.mapletree.aceris.Toml

val config = Toml.parse("""
    [server]
    host = "127.0.0.1"
    port = 8080
""".trimIndent())

val host = config.getString("server.host").orElseThrow()
val port = config.getInteger("server.port").orElse(80)
```

</details>

## API Guide

<details open>
<summary>Type mapping</summary>

| TOML value | Java/Kotlin target |
| --- | --- |
| Integer | `byte`, `Byte`, `short`, `Short`, `int`, `Integer`, `long`, `Long`, `BigInteger` |
| Float | `float`, `Float`, `double`, `Double`, `BigDecimal` |
| String | `String`, `enum`, `URI`, `URL`, `Duration` |
| One-letter string | `char`, `Character` |
| Multiline and literal strings | `String` |
| Boolean | `boolean`, `Boolean` |
| Offset date-time | `OffsetDateTime`, `Instant`, `Date`, `Timestamp` |
| Local date-time | `LocalDateTime` |
| Local date | `LocalDate`, `java.sql.Date` |
| Local time | `LocalTime` |
| Array | `List<T>`, `Set<T>`, `SortedSet<T>`, `EnumSet<E>`, `T[]`, primitive arrays |
| Table | `Map<String, Object>`, Java records, Java beans, immutable constructor classes |

```java
int port = config.getInteger("server.port").orElseThrow();
URI uri = config.getUri("server.uri").orElseThrow();
List<Integer> ports = config.getList("server.ports", Integer.class).orElseThrow();
ServerConfig server = config.getAs("server", ServerConfig.class).orElseThrow();

List<ServerConfig> servers = config
    .getAs("servers", new TomlType<List<ServerConfig>>() {})
    .orElseThrow();
```

</details>

<details>
<summary>Object mapping</summary>

Aceris maps TOML tables into records, beans, private fields, and immutable constructor classes.

```java
public record ServerConfig(String host, int port, Mode mode) {
}

public enum Mode {
    ALPHA,
    BETA
}

ServerConfig server = config.getAs("server", ServerConfig.class).orElseThrow();
```

Key matching supports exact names and snake_case/kebab-case TOML keys mapped to camelCase Java fields. Use annotations for explicit names and defaults:

```java
public record Contact(
    @TomlKey("email address") String emailAddress,
    @TomlDefault("guest") String role
) {
}
```

</details>

<details>
<summary>Writing TOML</summary>

`TomlWriter` serializes maps and Java objects into fresh TOML text.

```java
String toml = TomlWriter.write(Map.of(
    "title", "Example",
    "server", Map.of(
        "host", "127.0.0.1",
        "port", 8080
    )
));
```

Object serialization:

```java
public final class Config {
    @TomlSection("XY")
    private XY xy = new XY();

    @TomlIgnore
    private String secret = "hidden";

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

The writer supports primitives, strings, numbers, booleans, enums, temporal values, arrays, collections, maps, records, and simple objects. It does not preserve comments or original formatting; round-trip editing is a separate future layer.

</details>

<details>
<summary>Global config store</summary>

For applications, register config files once at startup and use typed configs globally.

```java
public final class Config {
    @TomlSection("XY")
    private XY xy = new XY();

    public static final class XY {
        private int test = 0;
    }
}
```

```java
TomlConfigStore store = TomlConfigStore.builder()
    .add(Path.of("config.toml"), Config.class)
    .add(Path.of("features.toml"), FeatureConfig.class)
    .build();

store.initializeAll(); // Creates missing files from Java defaults.

Config config = TomlConfig.get(Config.class);
TomlConfig.update(Config.class, current -> current.xy.test = 7);
TomlConfig.save(Config.class);
```

The store can register multiple files/classes and later call `saveAll()` or `reloadAll()`.

</details>

<details>
<summary>Script integration</summary>

Aceris can load script definitions from TOML, but it never executes anything implicitly and does not ship a shell engine. Execution is only possible through engines registered by the host application.

The script API lives in `dev.mapletree.aceris.script`.

```toml
[scripts.hello]
engine = "echo"
source = "hello"
options.cwd = "/tmp"
```

```java
import dev.mapletree.aceris.script.TomlScriptRunner;

TomlScriptRunner runner = new TomlScriptRunner()
    .register("echo", (script, context) -> script.source() + " " + context.get("name"));

Object result = runner.execute(document, "hello", Map.of("name", "Aceris"));
```

Unknown engines and malformed script definitions fail with `TomlScriptExecutionException`.

</details>

<details>
<summary>Stability and errors</summary>

`TomlOptions.DEFAULT` protects the parser with bounded limits:

- maximum document characters
- maximum value nesting depth
- maximum array items
- maximum inline table entries
- maximum string characters
- maximum scalar characters

Production callers can tighten those limits:

```java
TomlOptions options = new TomlOptions(
    1024 * 1024,
    64,
    10_000,
    10_000,
    256 * 1024,
    64 * 1024
);

TomlDocument config = Toml.parse(text, "app.toml", options);
```

Error types:

- `TomlParseException`: invalid TOML input
- `TomlConversionException`: invalid Java type conversion
- `TomlWriteException`: unsupported serialization value
- `TomlConfigException`: config-store file or lifecycle failure
- `TomlScriptExecutionException`: script lookup, engine, or execution failure

</details>

## Quality

<details open>
<summary>Build and test</summary>

Use the Gradle Wrapper:

```sh
./gradlew check
```

Run individual dependency-free test groups:

```sh
./gradlew integerTypeTest
./gradlew floatTypeTest
./gradlew stringTypeTest
./gradlew temporalTypeTest
./gradlew arrayCollectionTest
./gradlew tableScenarioTest
./gradlew objectMappingTest
./gradlew writerTest
./gradlew configTest
./gradlew scriptRunnerTest
./gradlew conformanceTest
./gradlew stabilityTest
./gradlew astTest
./gradlew crashTest
```

You can also compile and run without Gradle:

```sh
mkdir -p build/classes
javac -d build/classes $(find src/main/java src/test/java -name '*.java')
java -cp build/classes dev.mapletree.aceris.TomlTestSuite
```

</details>

<details>
<summary>Conformance</summary>

The official [`toml-lang/toml-test`](https://github.com/toml-lang/toml-test) fixture repository is vendored under `third_party/toml-test`.

`TomlConformanceTest` currently runs a selected TOML 1.0 fixture set for active hardening areas:

- integer syntax
- float syntax
- comments
- control characters
- keys
- arrays
- strings
- temporal validation

The fixture set is intentionally green and will be expanded as parser support grows.

</details>

<details>
<summary>Benchmarks</summary>

Aceris includes a dependency-free microbenchmark:

```sh
./gradlew benchmark
```

Short smoke run:

```sh
./gradlew benchmark -Daceris.benchmark.warmup=5 -Daceris.benchmark.iterations=10
```

Current scenarios:

- parse a small document
- parse and map a small document
- parse a large array
- parse many tables
- map many table objects

This is not a replacement for JMH. It is a lightweight baseline to spot obvious regressions before adding a full benchmark harness.

</details>

## Project Information

<details>
<summary>Project layout</summary>

```text
src/main/java/dev/mapletree/aceris          Library source
src/main/java/dev/mapletree/aceris/script   Script integration API
src/test/java/dev/mapletree/aceris          Dependency-free tests and benchmark
third_party/toml-test                       Vendored official TOML fixture repository
.github/workflows/ci.yml                    GitHub Actions workflow
docs/wiki                                   Extended documentation pages
```

</details>

<details>
<summary>Documentation</summary>

Detailed guide pages are available under `docs/wiki/`.

</details>

<details>
<summary>Security notes</summary>

- Parsing TOML never executes scripts.
- `TomlScriptRunner` only runs scripts through explicitly registered host engines.
- No shell engine is included.
- Parser limits are enabled by default.
- Invalid TOML and invalid conversions are reported through typed exceptions.

Security reports are handled through the process described in [SECURITY.md](SECURITY.md).

</details>

<details>
<summary>Roadmap</summary>

- Expand TOML 1.0 conformance coverage
- Add full Javadocs for public API types
- Add JMH benchmarks
- Add round-trip editing with comment and formatting preservation
- Add first-class Kotlin data class mapping once Kotlin reflection is intentionally introduced

</details>

## License

Aceris is released under the MIT License. You may use, copy, modify, publish, distribute, sublicense, and sell the software, including in commercial products, as long as the copyright and license notice are preserved.

See [LICENSE](LICENSE).
