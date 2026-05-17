package dev.mapletree.aceris;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TomlConformanceTest {
    private static final Path FIXTURES = Path.of("third_party", "toml-test", "tests");

    private static final List<String> VALID_1_0_FIXTURES = List.of(
        "valid/array/hetergeneous.toml",
        "valid/array/mixed-int-string.toml",
        "valid/array/trailing-comma.toml",
        "valid/comment/everywhere.toml",
        "valid/comment/nonascii.toml",
        "valid/integer/zero.toml",
        "valid/key/empty-01.toml",
        "valid/string/escapes.toml"
    );

    private static final List<String> INVALID_1_0_FIXTURES = List.of(
        "invalid/integer/leading-zero-01.toml",
        "invalid/integer/leading-zero-sign-01.toml",
        "invalid/integer/positive-hex.toml",
        "invalid/integer/negative-hex.toml",
        "invalid/float/leading-zero.toml",
        "invalid/float/leading-zero-neg.toml",
        "invalid/control/comment-null.toml",
        "invalid/control/string-null.toml",
        "invalid/control/rawstring-null.toml",
        "invalid/key/special-character.toml",
        "invalid/local-date/feb-30.toml",
        "invalid/datetime/hour-over.toml"
    );

    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlConformanceTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("parses selected official valid TOML 1.0 fixtures", TomlConformanceTest::parsesSelectedOfficialValidToml10Fixtures);
        TestSupport.run("rejects selected official invalid TOML 1.0 fixtures", TomlConformanceTest::rejectsSelectedOfficialInvalidToml10Fixtures);
    }

    private static void parsesSelectedOfficialValidToml10Fixtures() {
        requireFixtures();
        for (String fixture : VALID_1_0_FIXTURES) {
            String text = readFixture(fixture);
            try {
                Toml.parse(text, fixture);
            } catch (RuntimeException exception) {
                throw new AssertionError("Expected official valid fixture to parse: " + fixture, exception);
            }
        }
    }

    private static void rejectsSelectedOfficialInvalidToml10Fixtures() {
        requireFixtures();
        for (String fixture : INVALID_1_0_FIXTURES) {
            String text = readFixture(fixture);
            TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse(text, fixture));
        }
    }

    private static void requireFixtures() {
        if (!Files.isDirectory(FIXTURES)) {
            throw new AssertionError("Missing official TOML fixtures at " + FIXTURES);
        }
    }

    private static String readFixture(String fixture) {
        try {
            return Files.readString(FIXTURES.resolve(fixture));
        } catch (IOException exception) {
            throw new AssertionError("Cannot read fixture " + fixture, exception);
        }
    }
}
