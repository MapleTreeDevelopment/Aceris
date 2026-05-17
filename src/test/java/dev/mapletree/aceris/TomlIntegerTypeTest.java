package dev.mapletree.aceris;

import java.math.BigInteger;

public final class TomlIntegerTypeTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlIntegerTypeTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("parses integer formats", TomlIntegerTypeTest::parsesIntegerFormats);
        TestSupport.run("converts small integer types", TomlIntegerTypeTest::convertsSmallIntegerTypes);
        TestSupport.run("keeps arbitrary precision view of valid integers", TomlIntegerTypeTest::keepsArbitraryPrecisionViewOfValidIntegers);
        TestSupport.run("wraps integer conversion overflows", TomlIntegerTypeTest::wrapsIntegerConversionOverflows);
    }

    private static void parsesIntegerFormats() {
        TomlDocument document = Toml.parse("""
            decimal = 1_000
            hex = 0xFF
            octal = 0o755
            binary = 0b1010
            negative = -42
            """);

        TestSupport.assertEquals(1000L, document.getLong("decimal").orElseThrow());
        TestSupport.assertEquals(255L, document.getLong("hex").orElseThrow());
        TestSupport.assertEquals(493L, document.getLong("octal").orElseThrow());
        TestSupport.assertEquals(10L, document.getLong("binary").orElseThrow());
        TestSupport.assertEquals(-42L, document.getLong("negative").orElseThrow());
    }

    private static void convertsSmallIntegerTypes() {
        TomlDocument document = Toml.parse("""
            byte_value = 12
            short_value = 32000
            int_value = 42
            long_value = 9223372036854775807
            """);

        TestSupport.assertEquals((byte) 12, document.getByte("byte_value").orElseThrow());
        TestSupport.assertEquals((short) 32000, document.getShort("short_value").orElseThrow());
        TestSupport.assertEquals(42, document.getInteger("int_value").orElseThrow());
        TestSupport.assertEquals(9223372036854775807L, document.getLong("long_value").orElseThrow());
    }

    private static void keepsArbitraryPrecisionViewOfValidIntegers() {
        TomlDocument document = Toml.parse("big = 9223372036854775807");
        TestSupport.assertEquals(new BigInteger("9223372036854775807"), document.getBigInteger("big").orElseThrow());
    }

    private static void wrapsIntegerConversionOverflows() {
        TomlDocument document = Toml.parse("""
            byte_value = 128
            short_value = 32768
            int_value = 2147483648
            """);

        TestSupport.assertThrows(TomlConversionException.class, () -> document.getByte("byte_value").orElseThrow());
        TestSupport.assertThrows(TomlConversionException.class, () -> document.getShort("short_value").orElseThrow());
        TestSupport.assertThrows(TomlConversionException.class, () -> document.getInteger("int_value").orElseThrow());
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("long_value = 9223372036854775808"));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("leading_zero = 01"));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("positive_hex = +0xFF"));
        TestSupport.assertThrows(TomlParseException.class, () -> Toml.parse("negative_hex = -0xFF"));
    }
}
