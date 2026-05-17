package dev.mapletree.aceris;

import java.net.URI;
import java.time.Duration;

public final class TomlObjectMappingTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlObjectMappingTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("maps Java records", TomlObjectMappingTest::mapsJavaRecords);
        TestSupport.run("maps beans with normalized keys", TomlObjectMappingTest::mapsBeansWithNormalizedKeys);
        TestSupport.run("maps explicit quoted keys", TomlObjectMappingTest::mapsExplicitQuotedKeys);
        TestSupport.run("applies default values", TomlObjectMappingTest::appliesDefaultValues);
        TestSupport.run("maps immutable constructor classes", TomlObjectMappingTest::mapsImmutableConstructorClasses);
    }

    private static void mapsJavaRecords() {
        TomlDocument document = Toml.parse("""
            [server]
            host = "localhost"
            port = 8080
            mode = "ALPHA"
            """);

        TestModels.ServerConfig server = document.getAs("server", TestModels.ServerConfig.class).orElseThrow();
        TestSupport.assertEquals("localhost", server.host());
        TestSupport.assertEquals(8080, server.port());
        TestSupport.assertEquals(TestModels.Mode.ALPHA, server.mode());
    }

    private static void mapsBeansWithNormalizedKeys() {
        TomlDocument document = Toml.parse("""
            [naming]
            max-connections = 64
            api_token = "secret"
            "email address" = "ops@example.com"
            """);

        TestModels.NamingBean naming = document.getAs("naming", TestModels.NamingBean.class).orElseThrow();
        TestSupport.assertEquals(64, naming.maxConnections);
        TestSupport.assertEquals("secret", naming.apiToken);
        TestSupport.assertEquals("ops@example.com", naming.emailAddress);
    }

    private static void mapsExplicitQuotedKeys() {
        TomlDocument document = Toml.parse("""
            [contact]
            "email address" = "me@example.com"
            """);

        TestModels.Contact contact = document.getAs("contact", TestModels.Contact.class).orElseThrow();
        TestSupport.assertEquals("me@example.com", contact.emailAddress());
    }

    private static void appliesDefaultValues() {
        TomlDocument document = Toml.parse("""
            [contact]
            "email address" = "me@example.com"

            [naming]
            max-connections = 64
            api_token = "secret"
            "email address" = "ops@example.com"
            """);

        TestModels.Contact contact = document.getAs("contact", TestModels.Contact.class).orElseThrow();
        TestModels.NamingBean naming = document.getAs("naming", TestModels.NamingBean.class).orElseThrow();
        TestSupport.assertEquals("guest", contact.role());
        TestSupport.assertEquals(Duration.ofSeconds(30), naming.timeout);
    }

    private static void mapsImmutableConstructorClasses() {
        TomlDocument document = Toml.parse("""
            [endpoint]
            base-url = "https://example.com/api"
            """);

        TestModels.ImmutableEndpoint endpoint = document.getAs("endpoint", TestModels.ImmutableEndpoint.class).orElseThrow();
        TestSupport.assertEquals(URI.create("https://example.com/api"), endpoint.baseUrl);
        TestSupport.assertEquals(true, endpoint.enabled);
    }
}
