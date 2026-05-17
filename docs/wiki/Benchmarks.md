# Benchmarks

> A lightweight dependency-free benchmark provides quick feedback on parser and mapper changes.

## Run

```sh
./gradlew benchmark
```

## Short Smoke Run

```sh
./gradlew benchmark -Daceris.benchmark.warmup=5 -Daceris.benchmark.iterations=10
```

## Scenarios

| Scenario | Focus |
| --- | --- |
| Parse small document | Parser baseline |
| Parse and map small document | Parser plus mapper overhead |
| Parse large array | Array handling and allocation pressure |
| Parse many tables | Table management |
| Map many table objects | Object conversion throughput |

This benchmark is not a replacement for JMH. It is a lightweight baseline for spotting obvious regressions.

## Navigation

[Home](Home) | [TOML Conformance](TOML-Conformance) | [FAQ](FAQ)

