package dev.mapletree.aceris;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Date;

public final class TomlTemporalTypeTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlTemporalTypeTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("parses TOML temporal values", TomlTemporalTypeTest::parsesTomlTemporalValues);
        TestSupport.run("converts offset date time to legacy types", TomlTemporalTypeTest::convertsOffsetDateTimeToLegacyTypes);
        TestSupport.run("converts local date to SQL date", TomlTemporalTypeTest::convertsLocalDateToSqlDate);
    }

    private static void parsesTomlTemporalValues() {
        TomlDocument document = Toml.parse("""
            date = 2026-05-17
            time = 14:30:00
            local = 2026-05-17T14:30:00
            offset = 2026-05-17T14:30:00Z
            """);

        TestSupport.assertEquals(LocalDate.parse("2026-05-17"), document.getLocalDate("date").orElseThrow());
        TestSupport.assertEquals(LocalTime.parse("14:30:00"), document.getLocalTime("time").orElseThrow());
        TestSupport.assertEquals(LocalDateTime.parse("2026-05-17T14:30:00"), document.getLocalDateTime("local").orElseThrow());
        TestSupport.assertEquals(OffsetDateTime.parse("2026-05-17T14:30:00Z"), document.getOffsetDateTime("offset").orElseThrow());
    }

    private static void convertsOffsetDateTimeToLegacyTypes() {
        TomlDocument document = Toml.parse("stamp = 2026-05-17T14:30:00Z");
        Instant instant = Instant.parse("2026-05-17T14:30:00Z");

        TestSupport.assertEquals(instant, document.getInstant("stamp").orElseThrow());
        TestSupport.assertEquals(Date.from(instant), document.getDate("stamp").orElseThrow());
        TestSupport.assertEquals(Timestamp.from(instant), document.getTimestamp("stamp").orElseThrow());
    }

    private static void convertsLocalDateToSqlDate() {
        TomlDocument document = Toml.parse("date = 2026-05-17");
        TestSupport.assertEquals(java.sql.Date.valueOf(LocalDate.parse("2026-05-17")), document.getSqlDate("date").orElseThrow());
    }
}
