package dev.mapletree.aceris;

import java.math.BigDecimal;

public final class TomlFloatTypeTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlFloatTypeTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("parses decimal and exponent floats", TomlFloatTypeTest::parsesDecimalAndExponentFloats);
        TestSupport.run("keeps decimal precision", TomlFloatTypeTest::keepsDecimalPrecision);
        TestSupport.run("parses special float values", TomlFloatTypeTest::parsesSpecialFloatValues);
        TestSupport.run("rejects invalid float conversions", TomlFloatTypeTest::rejectsInvalidFloatConversions);
    }

    private static void parsesDecimalAndExponentFloats() {
        TomlDocument document = Toml.parse("""
            decimal = 12.5
            exponent = 1.5e2
            """);

        TestSupport.assertEquals(12.5d, document.getDouble("decimal").orElseThrow());
        TestSupport.assertEquals(150.0d, document.getDouble("exponent").orElseThrow());
        TestSupport.assertEquals(12.5f, document.getFloat("decimal").orElseThrow());
    }

    private static void keepsDecimalPrecision() {
        TomlDocument document = Toml.parse("precise = 0.12345678901234567890123456789");
        TestSupport.assertEquals(new BigDecimal("0.12345678901234567890123456789"), document.getBigDecimal("precise").orElseThrow());
    }

    private static void parsesSpecialFloatValues() {
        TomlDocument document = Toml.parse("""
            positive = inf
            negative = -inf
            missing = nan
            """);

        TestSupport.assertEquals(Double.POSITIVE_INFINITY, document.getDouble("positive").orElseThrow());
        TestSupport.assertEquals(Double.NEGATIVE_INFINITY, document.getDouble("negative").orElseThrow());
        TestSupport.assertTrue(Double.isNaN(document.getDouble("missing").orElseThrow()));
    }

    private static void rejectsInvalidFloatConversions() {
        TomlDocument document = Toml.parse("value = inf");
        TestSupport.assertThrows(TomlConversionException.class, () -> document.getBigDecimal("value").orElseThrow());
    }
}
