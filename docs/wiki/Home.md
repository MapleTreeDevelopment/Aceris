# Aceris Wiki

> Dependency-free TOML tooling for Java and Kotlin applications.

Aceris provides a strict parser, typed document API, object mapper, writer, global config store, explicit script integration, conformance fixtures, crash tests, and lightweight benchmarks.

## Overview

| Area | Guide |
| --- | --- |
| First steps | [Getting Started](Getting-Started) |
| Project setup | [Installation](Installation) |
| Reading TOML | [Parsing TOML](Parsing-TOML) |
| Java/Kotlin conversions | [Type Mapping](Type-Mapping) |
| Mapping classes and records | [Object Mapping](Object-Mapping) |
| Serialization | [Writing TOML](Writing-TOML) |
| Application configs | [Global Config Store](Global-Config-Store) |
| Host-controlled scripts | [Script Integration](Script-Integration) |
| Failures and diagnostics | [Error Handling](Error-Handling) |
| Defensive parsing | [Parser Safety Limits](Parser-Safety-Limits) |
| Specification coverage | [TOML Conformance](TOML-Conformance) |
| Performance checks | [Benchmarks](Benchmarks) |
| Common questions | [FAQ](FAQ) |

## Design Principles

| Principle | Meaning |
| --- | --- |
| Dependency-free core | Aceris can be embedded without pulling a runtime graph into the host app. |
| Explicit failures | Parse, conversion, write, config, and script failures use typed exceptions. |
| Java-first, Kotlin-friendly | APIs work naturally from Java and remain straightforward from Kotlin. |
| Host-controlled scripts | TOML may describe scripts, but only registered application engines can execute them. |
| Incremental conformance | Official TOML fixtures are expanded in small, reviewable steps. |

## Recommended Reading Path

1. [Getting Started](Getting-Started)
2. [Type Mapping](Type-Mapping)
3. [Object Mapping](Object-Mapping)
4. [Writing TOML](Writing-TOML)
5. [Parser Safety Limits](Parser-Safety-Limits)

