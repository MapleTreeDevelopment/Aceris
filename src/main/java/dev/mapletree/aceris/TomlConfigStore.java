package dev.mapletree.aceris;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Declarative list of TOML config files and their Java classes.
 */
public final class TomlConfigStore {
    private final List<Registration<?>> registrations;

    private TomlConfigStore(List<Registration<?>> registrations) {
        this.registrations = List.copyOf(registrations);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void initializeAll() {
        for (Registration<?> registration : registrations) {
            initialize(registration);
        }
    }

    public void reloadAll() {
        for (Registration<?> registration : registrations) {
            TomlConfig.reload(registration.type());
        }
    }

    public void saveAll() {
        for (Registration<?> registration : registrations) {
            TomlConfig.save(registration.type());
        }
    }

    public List<Class<?>> types() {
        List<Class<?>> types = new ArrayList<>();
        for (Registration<?> registration : registrations) {
            types.add(registration.type());
        }
        return List.copyOf(types);
    }

    private static <T> void initialize(Registration<T> registration) {
        TomlConfig.initialize(registration.path(), registration.type());
    }

    private record Registration<T>(Path path, Class<T> type) {
    }

    public static final class Builder {
        private final List<Registration<?>> registrations = new ArrayList<>();

        private Builder() {
        }

        public <T> Builder add(Path path, Class<T> type) {
            if (path == null) {
                throw new IllegalArgumentException("path must not be null");
            }
            if (type == null) {
                throw new IllegalArgumentException("type must not be null");
            }
            registrations.add(new Registration<>(path, type));
            return this;
        }

        public TomlConfigStore build() {
            return new TomlConfigStore(registrations);
        }
    }
}
