# Global Config Store

> Register file-backed config classes once and access them globally by type.

## Core Types

| Type | Purpose |
| --- | --- |
| `TomlConfigStore` | Registers multiple config files and classes. |
| `TomlConfig` | Provides global typed access, save, reload, and update operations. |

## Config Class

```java
public final class Config {
    @TomlSection("XY")
    private XY xy = new XY();

    public static final class XY {
        private int test = 0;
    }
}
```

## Register Files

```java
TomlConfigStore store = TomlConfigStore.builder()
    .add(Path.of("config.toml"), Config.class)
    .add(Path.of("features.toml"), FeatureConfig.class)
    .build();

store.initializeAll();
```

Missing files are created from Java defaults.

## Access and Save

```java
Config config = TomlConfig.get(Config.class);
TomlConfig.update(Config.class, current -> current.xy.test = 7);
TomlConfig.save(Config.class);
```

## Reload

```java
TomlConfig.reload(Config.class);
store.reloadAll();
```

## Navigation

[Home](Home) | [Writing TOML](Writing-TOML) | [Parser Safety Limits](Parser-Safety-Limits)

