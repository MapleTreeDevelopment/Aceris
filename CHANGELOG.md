# Changelog

All notable changes to Aceris will be documented in this file.

This project uses semantic versioning for public releases.

## [Unreleased]

### Added

- Dependency-free TOML parser and interpreter for Java and Kotlin.
- Typed `TomlDocument` API.
- Object mapping for records, beans, private fields, and immutable constructor classes.
- Mapping annotations: `@TomlKey`, `@TomlSection`, `@TomlDefault`, and `@TomlIgnore`.
- TOML writer for maps and Java objects.
- Global file-backed config store via `TomlConfig` and `TomlConfigStore`.
- Explicit script integration through registered host engines.
- Parser safety limits through `TomlOptions`.
- Selected official TOML 1.0 conformance fixtures via vendored `toml-lang/toml-test`.
- Dependency-free test suite, crash test, and microbenchmark.
- Gradle Wrapper and GitHub Actions CI workflow.
- Release bundle workflow for GitHub Actions.

### Notes

- Full TOML 1.0 conformance coverage is still being expanded.
- Round-trip editing with comment and formatting preservation is not implemented yet.
