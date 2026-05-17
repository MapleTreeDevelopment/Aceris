# Installation

> Current setup options for source builds, GitHub artifacts, and the planned Maven coordinate.

## Status

| Channel | Status |
| --- | --- |
| Source build | Available |
| GitHub release JARs | Available after tagged releases |
| Maven Central | Planned |

## Planned Maven Coordinate

```kotlin
implementation("dev.mapletree:aceris:0.1.0")
```

If the first release uses the GitHub namespace, the coordinate will use:

```kotlin
implementation("io.github.mapletreedevelopment:aceris:0.1.0")
```

## Build from Source

```sh
./gradlew build
```

## Composite Build

```kotlin
includeBuild("../Aceris")
```

Then depend on the published project coordinate used by the included build.

## Navigation

[Home](Home) | [Getting Started](Getting-Started) | [FAQ](FAQ)

