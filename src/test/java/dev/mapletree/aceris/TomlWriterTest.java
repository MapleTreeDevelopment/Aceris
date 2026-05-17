package dev.mapletree.aceris;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class TomlWriterTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlWriterTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("writes maps", TomlWriterTest::writesMaps);
        TestSupport.run("writes private object fields as sections", TomlWriterTest::writesPrivateObjectFieldsAsSections);
        TestSupport.run("writes records and arrays of tables", TomlWriterTest::writesRecordsAndArraysOfTables);
        TestSupport.run("honors key section and ignore annotations", TomlWriterTest::honorsKeySectionAndIgnoreAnnotations);
        TestSupport.run("roundtrips serialized objects", TomlWriterTest::roundtripsSerializedObjects);
        TestSupport.run("rejects unsupported write values", TomlWriterTest::rejectsUnsupportedWriteValues);
    }

    private static void writesMaps() {
        String toml = TomlWriter.write(Map.of(
            "title", "Example",
            "enabled", true,
            "count", 3,
            "price", new BigDecimal("12.50"),
            "date", LocalDate.parse("2026-05-17"),
            "server", Map.of("host", "127.0.0.1", "port", 8080),
            "quoted key", "yes"
        ));

        TomlDocument document = Toml.parse(toml);
        TestSupport.assertEquals("Example", document.getString("title").orElseThrow());
        TestSupport.assertEquals(true, document.getBoolean("enabled").orElseThrow());
        TestSupport.assertEquals(3, document.getInteger("count").orElseThrow());
        TestSupport.assertEquals(new BigDecimal("12.50"), document.getBigDecimal("price").orElseThrow());
        TestSupport.assertEquals(LocalDate.parse("2026-05-17"), document.getLocalDate("date").orElseThrow());
        TestSupport.assertEquals("127.0.0.1", document.getString("server.host").orElseThrow());
        TestSupport.assertEquals(8080, document.getInteger("server.port").orElseThrow());
        TestSupport.assertEquals("yes", document.getString("quoted key").orElseThrow());
    }

    private static void writesPrivateObjectFieldsAsSections() {
        AppConfig config = new AppConfig();
        String toml = TomlWriter.writeObject(config);

        TestSupport.assertTrue(toml.contains("[server]"));
        TestSupport.assertTrue(toml.contains("port = 8080"));
        TestSupport.assertTrue(toml.contains("host = \"127.0.0.1\""));

        TomlDocument document = Toml.parse(toml);
        TestSupport.assertEquals(8080, document.getInteger("server.port").orElseThrow());
        TestSupport.assertEquals("127.0.0.1", document.getString("server.host").orElseThrow());
    }

    private static void writesRecordsAndArraysOfTables() {
        Cluster cluster = new Cluster(List.of(
            new Node("alpha", 8001),
            new Node("beta", 8002)
        ));

        String toml = TomlWriter.writeObject(cluster);

        TestSupport.assertTrue(toml.contains("[[nodes]]"));
        TomlDocument document = Toml.parse(toml);
        List<Node> nodes = document.getAs("nodes", new TomlType<List<Node>>() {
        }).orElseThrow();
        TestSupport.assertEquals("alpha", nodes.get(0).name());
        TestSupport.assertEquals(8002, nodes.get(1).port());
    }

    private static void honorsKeySectionAndIgnoreAnnotations() {
        AnnotatedConfig config = new AnnotatedConfig();
        String toml = TomlWriter.writeObject(config);

        TestSupport.assertTrue(toml.contains("[XY]"));
        TestSupport.assertTrue(toml.contains("\"email address\" = \"ops@example.com\""));
        TestSupport.assertTrue(!toml.contains("secret"));

        TomlDocument document = Toml.parse(toml);
        TestSupport.assertEquals(0, document.getInteger("XY.test").orElseThrow());
        TestSupport.assertEquals("ops@example.com", document.getString("email address").orElseThrow());
    }

    private static void roundtripsSerializedObjects() {
        AppConfig config = new AppConfig();
        String toml = TomlWriter.writeObject(config);
        AppConfig mapped = Toml.parse(toml).to(AppConfig.class);

        TestSupport.assertEquals(config.server.port, mapped.server.port);
        TestSupport.assertEquals(config.server.host, mapped.server.host);
        TestSupport.assertEquals(config.serviceUri, mapped.serviceUri);
    }

    private static void rejectsUnsupportedWriteValues() {
        TestSupport.assertThrows(TomlWriteException.class, () -> TomlWriter.write(Map.of("bad", new Object())));
        TestSupport.assertThrows(IllegalArgumentException.class, () -> TomlWriter.writeObject(null));
    }

    private static final class AppConfig {
        @TomlSection("server")
        private Server server = new Server();
        private URI serviceUri = URI.create("https://example.com/api");
    }

    private static final class Server {
        private int port = 8080;
        private String host = "127.0.0.1";
    }

    private static final class AnnotatedConfig {
        @TomlSection("XY")
        private XY xy = new XY();
        @TomlKey("email address")
        private String emailAddress = "ops@example.com";
        @TomlIgnore
        private String secret = "hidden";
    }

    private static final class XY {
        private int test = 0;
    }

    private record Cluster(List<Node> nodes) {
    }

    private record Node(String name, int port) {
    }
}
