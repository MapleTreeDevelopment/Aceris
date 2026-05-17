# FAQ

> Short answers to common Aceris questions.

## Is Aceris dependency-free?

Yes. The core library has no runtime dependencies.

## Does parsing execute scripts?

No. Parsing TOML never executes scripts. Script execution requires an explicitly registered host engine.

## Does the writer preserve comments?

No. `TomlWriter` generates fresh TOML from values. Comment-preserving round-trip editing is planned as a separate layer.

## Can Kotlin use Aceris?

Yes. Aceris is Java-first and Kotlin-friendly. First-class Kotlin data class mapping may be added later with Kotlin reflection.

## Is full TOML 1.0 conformance complete?

The parser targets TOML 1.0 and includes selected official fixtures. Full fixture coverage is being expanded incrementally.

## Which Java version is required?

Java 17 or newer.

## Navigation

[Home](Home) | [Getting Started](Getting-Started) | [Installation](Installation)

