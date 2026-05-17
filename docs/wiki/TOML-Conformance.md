# TOML Conformance

> Aceris targets TOML 1.0 behavior and uses official fixtures for incremental coverage.

## Fixture Source

Official fixtures from `toml-lang/toml-test` are vendored under:

```text
third_party/toml-test
```

## Current Coverage Areas

| Area | Covered by selected fixtures |
| --- | --- |
| Integer syntax | Yes |
| Float syntax | Yes |
| Comments | Yes |
| Control characters | Yes |
| Keys | Yes |
| Arrays | Yes |
| Strings | Yes |
| Temporal validation | Yes |

## Expanding Coverage

1. Add selected fixture paths to `TomlConformanceTest`.
2. Fix parser behavior until the selected set is green.
3. Keep fixture additions small and reviewable.

## Navigation

[Home](Home) | [Benchmarks](Benchmarks) | [Parser Safety Limits](Parser-Safety-Limits)

