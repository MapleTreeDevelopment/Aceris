# Script Integration

> TOML can describe scripts, but execution is always explicit and host-controlled.

## Safety Model

| Rule | Behavior |
| --- | --- |
| Parsing | Never executes scripts. |
| Engines | Only host-registered engines can run. |
| Shell | No shell engine is included. |
| Unknown engine | Fails with `TomlScriptExecutionException`. |

## API Package

```java
dev.mapletree.aceris.script
```

## TOML Shape

```toml
[scripts.hello]
engine = "echo"
source = "hello"
options.cwd = "/tmp"
```

## Register an Engine

```java
TomlScriptRunner runner = new TomlScriptRunner()
    .register("echo", (script, context) -> script.source() + " " + context.get("name"));

Object result = runner.execute(document, "hello", Map.of("name", "Aceris"));
```

## Navigation

[Home](Home) | [Error Handling](Error-Handling) | [Parser Safety Limits](Parser-Safety-Limits)

