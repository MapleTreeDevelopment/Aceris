package dev.mapletree.aceris;

import java.util.List;
import java.util.Map;

public final class TomlTableScenarioTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlTableScenarioTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("parses regular tables and dotted keys", TomlTableScenarioTest::parsesRegularTablesAndDottedKeys);
        TestSupport.run("parses inline tables", TomlTableScenarioTest::parsesInlineTables);
        TestSupport.run("parses quoted keys with spaces", TomlTableScenarioTest::parsesQuotedKeysWithSpaces);
        TestSupport.run("parses nested arrays of tables", TomlTableScenarioTest::parsesNestedArraysOfTables);
        TestSupport.run("rejects duplicate keys and tables", TomlTableScenarioTest::rejectsDuplicateKeysAndTables);
    }

    private static void parsesRegularTablesAndDottedKeys() {
        TomlDocument document = Toml.parse("""
            title = "Demo"

            [server]
            host = "localhost"
            port = 8080

            [database]
            pool.max = 20
            """);

        TestSupport.assertEquals("Demo", document.getString("title").orElseThrow());
        TestSupport.assertEquals("localhost", document.getString("server.host").orElseThrow());
        TestSupport.assertEquals(8080L, document.getLong("server.port").orElseThrow());
        TestSupport.assertEquals(20L, document.getLong("database.pool.max").orElseThrow());
    }

    private static void parsesInlineTables() {
        TomlDocument document = Toml.parse("owner = { name = \"Example User\", org.role = \"admin\" }");
        Map<String, Object> owner = document.getTable("owner").orElseThrow();
        TestSupport.assertEquals("Example User", owner.get("name"));
        TestSupport.assertEquals("admin", ((Map<?, ?>) owner.get("org")).get("role"));
    }

    private static void parsesQuotedKeysWithSpaces() {
        TomlDocument document = Toml.parse("""
            name = "Mwanji Ezana"

            [contacts]
              "email address" = "me@example.com"
            """);

        TestSupport.assertEquals("Mwanji Ezana", document.getString("name").orElseThrow());
        TestSupport.assertEquals("me@example.com", document.getString("contacts.email address").orElseThrow());
    }

    private static void parsesNestedArraysOfTables() {
        TomlDocument document = Toml.parse("""
            [[networks]]
              name = "Level 1"
              [networks.status]
                bandwidth = 10

            [[networks]]
              name = "Level 2"

            [[networks]]
              name = "Level 3"
              [[networks.operators]]
                location = "Geneva"
              [[networks.operators]]
                location = "Paris"
            """);

        List<Object> networks = document.getArray("networks").orElseThrow();
        TestSupport.assertEquals(3, networks.size());
        Map<?, ?> firstNetwork = (Map<?, ?>) networks.get(0);
        Map<?, ?> thirdNetwork = (Map<?, ?>) networks.get(2);
        TestSupport.assertEquals("Level 1", firstNetwork.get("name"));
        TestSupport.assertEquals(10L, ((Map<?, ?>) firstNetwork.get("status")).get("bandwidth"));
        TestSupport.assertEquals("Level 3", thirdNetwork.get("name"));
        List<?> operators = (List<?>) thirdNetwork.get("operators");
        TestSupport.assertEquals("Geneva", ((Map<?, ?>) operators.get(0)).get("location"));
        TestSupport.assertEquals("Paris", ((Map<?, ?>) operators.get(1)).get("location"));
    }

    private static void rejectsDuplicateKeysAndTables() {
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("name = 'a'\nname = 'b'"));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("[server]\nport = 1\n[server]\nhost = 'x'"));
    }
}
