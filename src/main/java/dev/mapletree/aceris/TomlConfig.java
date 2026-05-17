package dev.mapletree.aceris;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Global typed configuration store backed by TOML files.
 */
public final class TomlConfig {
    private static final Map<Class<?>, Entry<?>> ENTRIES = new LinkedHashMap<>();

    private TomlConfig() {
    }

    public static synchronized <T> T initialize(Class<T> type) {
        return initialize(Path.of(defaultFilename(type)), type);
    }

    public static synchronized <T> T initialize(Path path, Class<T> type) {
        requireType(type);
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }

        T value;
        if (Files.exists(path)) {
            value = read(path, type);
        } else {
            value = instantiate(type);
            write(path, value);
        }
        ENTRIES.put(type, new Entry<>(path, type, value));
        return value;
    }

    public static synchronized <T> boolean isInitialized(Class<T> type) {
        requireType(type);
        return ENTRIES.containsKey(type);
    }

    public static synchronized <T> T get(Class<T> type) {
        return entry(type).value();
    }

    public static synchronized <T> void set(Class<T> type, T value) {
        requireType(type);
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        Entry<T> entry = entry(type);
        ENTRIES.put(type, new Entry<>(entry.path(), type, value));
    }

    public static synchronized <T> T update(Class<T> type, Consumer<T> updater) {
        if (updater == null) {
            throw new IllegalArgumentException("updater must not be null");
        }
        T value = get(type);
        updater.accept(value);
        return value;
    }

    public static synchronized <T> T reload(Class<T> type) {
        Entry<T> entry = entry(type);
        T value = read(entry.path(), type);
        ENTRIES.put(type, new Entry<>(entry.path(), type, value));
        return value;
    }

    public static synchronized <T> void save(Class<T> type) {
        Entry<T> entry = entry(type);
        write(entry.path(), entry.value());
    }

    public static synchronized void saveAll() {
        for (Entry<?> entry : ENTRIES.values()) {
            writeUntyped(entry.path(), entry.value());
        }
    }

    public static synchronized void clear() {
        ENTRIES.clear();
    }

    private static <T> T read(Path path, Class<T> type) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            return Toml.parse(text, path.toString()).to(type);
        } catch (IOException exception) {
            throw new TomlConfigException("Cannot read TOML config " + path, exception);
        }
    }

    private static <T> void write(Path path, T value) {
        writeUntyped(path, value);
    }

    private static void writeUntyped(Path path, Object value) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, TomlWriter.writeObject(value), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new TomlConfigException("Cannot write TOML config " + path, exception);
        }
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new TomlConfigException("Cannot create default config for " + type.getName(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Entry<T> entry(Class<T> type) {
        requireType(type);
        Entry<?> entry = ENTRIES.get(type);
        if (entry == null) {
            throw new TomlConfigException("Config type is not initialized: " + type.getName());
        }
        return (Entry<T>) entry;
    }

    private static void requireType(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
    }

    private static String defaultFilename(Class<?> type) {
        return type.getSimpleName().isBlank()
            ? "config.toml"
            : type.getSimpleName().toLowerCase() + ".toml";
    }

    private record Entry<T>(Path path, Class<T> type, T value) {
    }
}
