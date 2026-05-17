package dev.mapletree.aceris;

import java.net.URI;
import java.time.Duration;

final class TestModels {
    private TestModels() {
    }

    enum Mode {
        ALPHA,
        BETA
    }

    record ServerConfig(String host, int port, Mode mode) {
    }

    record Contact(@TomlKey("email address") String emailAddress, @TomlDefault("guest") String role) {
    }

    static final class NamingBean {
        int maxConnections;
        String apiToken;
        @TomlKey("email address")
        String emailAddress;
        @TomlDefault("PT30S")
        Duration timeout;
    }

    static final class ImmutableEndpoint {
        final URI baseUrl;
        final boolean enabled;

        ImmutableEndpoint(@TomlKey("base-url") URI baseUrl, @TomlDefault("true") boolean enabled) {
            this.baseUrl = baseUrl;
            this.enabled = enabled;
        }
    }
}
