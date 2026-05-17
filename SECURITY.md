# Security Policy

## Supported Versions

Aceris is pre-1.0 and under active development. Security fixes are applied to the main development branch until formal release lines exist.

## Reporting a Vulnerability

Please do not open a public issue for a suspected security vulnerability.

Until a dedicated security contact is published, report privately through the repository owner contact associated with MapleTreeDevelopment.

When reporting, include:

- Affected version or commit.
- A minimal reproduction.
- Expected and actual behavior.
- Impact assessment if known.

## Security Design Notes

- Parsing TOML never executes scripts.
- `TomlScriptRunner` only executes through engines explicitly registered by the host application.
- Aceris does not include a shell execution engine.
- Parser safety limits are enabled by default through `TomlOptions`.
- Invalid TOML, invalid conversions, writer failures, and script failures use typed exceptions.
