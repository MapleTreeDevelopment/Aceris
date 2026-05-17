# Contributing to Aceris

Thanks for your interest in contributing.

## Development Requirements

- JDK 17 or newer
- Git
- No external build tool installation is required; use the Gradle Wrapper.

## Build and Test

Run the full check:

```sh
./gradlew check
```

Run selected test groups:

```sh
./gradlew conformanceTest
./gradlew crashTest
./gradlew writerTest
./gradlew configTest
```

Run a short benchmark smoke test:

```sh
./gradlew benchmark -Daceris.benchmark.warmup=5 -Daceris.benchmark.iterations=10
```

## Code Style

- Keep the core dependency-free.
- Target Java 17 compatibility.
- Prefer clear, small APIs over broad abstractions.
- Add focused tests for every parser, mapper, writer, config-store, or stability change.
- Preserve typed exception behavior:
  - `TomlParseException` for invalid TOML.
  - `TomlConversionException` for invalid Java type conversion.
  - `TomlWriteException` for unsupported serialization.
  - `TomlConfigException` for config-store failures.
  - `TomlScriptExecutionException` for script execution failures.

## Conformance Fixtures

Official TOML fixtures live under `third_party/toml-test`.

When expanding conformance coverage:

1. Add selected fixtures to `TomlConformanceTest`.
2. Fix parser behavior until the selected set is green.
3. Keep fixture additions small and reviewable.

## Pull Requests

Before opening a pull request:

1. Run `./gradlew check`.
2. Include tests for behavior changes.
3. Update `README.md` or `CHANGELOG.md` when public behavior changes.
4. Keep unrelated refactors out of the PR.
