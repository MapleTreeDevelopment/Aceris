package dev.mapletree.aceris;

public final class TomlStabilityTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlStabilityTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("handles null and empty inputs", TomlStabilityTest::handlesNullAndEmptyInputs);
        TestSupport.run("validates API arguments", TomlStabilityTest::validatesApiArguments);
        TestSupport.run("wraps invalid conversions", TomlStabilityTest::wrapsInvalidConversions);
        TestSupport.run("rejects invalid unicode escapes", TomlStabilityTest::rejectsInvalidUnicodeEscapes);
        TestSupport.run("enforces parser limits", TomlStabilityTest::enforcesParserLimits);
        TestSupport.run("reports malformed syntax", TomlStabilityTest::reportsMalformedSyntax);
    }

    private static void handlesNullAndEmptyInputs() {
        TestSupport.assertTrue(Toml.parse(null).toMap().isEmpty());
        TestSupport.assertTrue(Toml.parse("").toMap().isEmpty());
        TestSupport.assertTrue(Toml.parse("   \n# only comment").toMap().isEmpty());
    }

    private static void validatesApiArguments() {
        TomlDocument document = Toml.parse("number = 1");
        TestSupport.assertThrows(IllegalArgumentException.class, () -> document.getString(""));
        TestSupport.assertThrows(IllegalArgumentException.class, () -> document.getString("   "));
        TestSupport.assertThrows(IllegalArgumentException.class, () -> document.getAs("number", (Class<?>) null));
        TestSupport.assertThrows(IllegalArgumentException.class, () -> document.getAs("number", (TomlType<?>) null));
    }

    private static void wrapsInvalidConversions() {
        TomlDocument document = Toml.parse("""
            number = 1
            uri = "not a uri with spaces"
            duration = "nope"
            values = [1, 2]
            """);

        TestSupport.assertThrows(TomlConversionException.class, () -> document.getUri("uri").orElseThrow());
        TestSupport.assertThrows(TomlConversionException.class, () -> document.getDuration("duration").orElseThrow());
        TestSupport.assertThrows(TomlConversionException.class, () -> document.getInteger("values").orElseThrow());
    }

    private static void rejectsInvalidUnicodeEscapes() {
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("bad = \"\\UFFFFFFFF\""));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("\"\\UD800\" = 1"));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("bad = \"\\uZZZZ\""));
    }

    private static void enforcesParserLimits() {
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("value = [1, 2, 3]", new TomlOptions(100, 10, 2, 10, 100, 100)));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("value = [[[1]]]", new TomlOptions(100, 2, 10, 10, 100, 100)));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("value = \"abcdef\"", new TomlOptions(100, 10, 10, 10, 3, 100)));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("value = 123456", new TomlOptions(100, 10, 10, 10, 100, 3)));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("value = 1", new TomlOptions(5, 10, 10, 10, 100, 100)));
    }

    private static void reportsMalformedSyntax() {
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("name"));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("[server"));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("values = [1,"));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("table = { a = 1"));
    }
}
