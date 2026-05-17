package dev.mapletree.aceris;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TomlConfigTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlConfigTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("initializes missing config with defaults", TomlConfigTest::initializesMissingConfigWithDefaults);
        TestSupport.run("loads existing config globally", TomlConfigTest::loadsExistingConfigGlobally);
        TestSupport.run("updates saves and reloads config", TomlConfigTest::updatesSavesAndReloadsConfig);
        TestSupport.run("initializes config store registrations", TomlConfigTest::initializesConfigStoreRegistrations);
        TestSupport.run("fails before initialization", TomlConfigTest::failsBeforeInitialization);
    }

    private static void initializesMissingConfigWithDefaults() {
        TomlConfig.clear();
        Path path = tempPath("defaults.toml");

        Config config = TomlConfig.initialize(path, Config.class);

        TestSupport.assertEquals(0, config.xy.test);
        TestSupport.assertTrue(Files.exists(path));
        TestSupport.assertEquals(0, TomlConfig.get(Config.class).xy.test);
    }

    private static void loadsExistingConfigGlobally() {
        TomlConfig.clear();
        Path path = tempPath("existing.toml");
        write(path, """
            [XY]
            test = 42
            """);

        Config config = TomlConfig.initialize(path, Config.class);

        TestSupport.assertEquals(42, config.xy.test);
        TestSupport.assertEquals(42, TomlConfig.get(Config.class).xy.test);
    }

    private static void updatesSavesAndReloadsConfig() {
        TomlConfig.clear();
        Path path = tempPath("save.toml");
        TomlConfig.initialize(path, Config.class);

        TomlConfig.update(Config.class, config -> config.xy.test = 7);
        TomlConfig.save(Config.class);
        TomlConfig.update(Config.class, config -> config.xy.test = 100);
        TomlConfig.reload(Config.class);

        TestSupport.assertEquals(7, TomlConfig.get(Config.class).xy.test);
    }

    private static void initializesConfigStoreRegistrations() {
        TomlConfig.clear();
        Path configPath = tempPath("store-config.toml");
        Path featurePath = tempPath("store-feature.toml");
        write(featurePath, """
            enabled = true
            """);

        TomlConfigStore store = TomlConfigStore.builder()
            .add(configPath, Config.class)
            .add(featurePath, FeatureConfig.class)
            .build();

        store.initializeAll();

        TestSupport.assertTrue(Files.exists(configPath));
        TestSupport.assertEquals(0, TomlConfig.get(Config.class).xy.test);
        TestSupport.assertEquals(true, TomlConfig.get(FeatureConfig.class).enabled);
        TestSupport.assertEquals(2, store.types().size());
    }

    private static void failsBeforeInitialization() {
        TomlConfig.clear();
        TestSupport.assertThrows(TomlConfigException.class, () -> TomlConfig.get(Config.class));
    }

    private static Path tempPath(String file) {
        try {
            Path directory = Files.createTempDirectory("aceris-config-test");
            directory.toFile().deleteOnExit();
            return directory.resolve(file);
        } catch (IOException exception) {
            throw new AssertionError("Cannot create temp directory", exception);
        }
    }

    private static void write(Path path, String text) {
        try {
            Files.writeString(path, text);
        } catch (IOException exception) {
            throw new AssertionError("Cannot write test file", exception);
        }
    }

    public static final class Config {
        @TomlSection("XY")
        private XY xy = new XY();

        public static final class XY {
            private int test = 0;
        }
    }

    public static final class FeatureConfig {
        private boolean enabled = false;
    }
}
