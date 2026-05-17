package dev.mapletree.aceris;

import java.net.URI;
import java.time.Duration;

public final class TomlStringTypeTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlStringTypeTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("parses basic and escaped strings", TomlStringTypeTest::parsesBasicAndEscapedStrings);
        TestSupport.run("parses literal and multiline strings", TomlStringTypeTest::parsesLiteralAndMultilineStrings);
        TestSupport.run("converts string backed types", TomlStringTypeTest::convertsStringBackedTypes);
        TestSupport.run("rejects invalid string backed conversions", TomlStringTypeTest::rejectsInvalidStringBackedConversions);
    }

    private static void parsesBasicAndEscapedStrings() {
        TomlDocument document = Toml.parse("""
            name = "Mwanji Ezana"
            escaped = "line\\nnext"
            unicode = "\\u0041"
            one = "x"
            """);

        TestSupport.assertEquals("Mwanji Ezana", document.getString("name").orElseThrow());
        TestSupport.assertEquals("line\nnext", document.getString("escaped").orElseThrow());
        TestSupport.assertEquals("A", document.getString("unicode").orElseThrow());
        TestSupport.assertEquals('x', document.getCharacter("one").orElseThrow());
    }

    private static void parsesLiteralAndMultilineStrings() {
        TomlDocument document = Toml.parse("""
            literal = '''literal \\ stays'''
            multiline = \"\"\"
            hello
            world
            \"\"\"
            """);

        TestSupport.assertEquals("literal \\ stays", document.getString("literal").orElseThrow());
        TestSupport.assertEquals("hello\nworld\n", document.getString("multiline").orElseThrow());
    }

    private static void convertsStringBackedTypes() {
        TomlDocument document = Toml.parse("""
            mode = "ALPHA"
            uri = "https://example.com/service"
            url = "https://example.com/page"
            duration = "PT45S"
            """);

        TestSupport.assertEquals(TestModels.Mode.ALPHA, document.getEnum("mode", TestModels.Mode.class).orElseThrow());
        TestSupport.assertEquals(URI.create("https://example.com/service"), document.getUri("uri").orElseThrow());
        TestSupport.assertEquals("https://example.com/page", document.getUrl("url").orElseThrow().toString());
        TestSupport.assertEquals(Duration.ofSeconds(45), document.getDuration("duration").orElseThrow());
    }

    private static void rejectsInvalidStringBackedConversions() {
        TomlDocument document = Toml.parse("""
            many = "ab"
            mode = "GAMMA"
            uri = "not a uri with spaces"
            duration = "nope"
            """);

        TestSupport.assertThrows(TomlConversionException.class, () -> document.getCharacter("many").orElseThrow());
        TestSupport.assertThrows(TomlConversionException.class, () -> document.getEnum("mode", TestModels.Mode.class).orElseThrow());
        TestSupport.assertThrows(TomlConversionException.class, () -> document.getUri("uri").orElseThrow());
        TestSupport.assertThrows(TomlConversionException.class, () -> document.getDuration("duration").orElseThrow());
    }
}
