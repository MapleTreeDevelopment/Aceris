package dev.mapletree.aceris;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serializes maps and Java objects to TOML.
 */
public final class TomlWriter {
    private TomlWriter() {
    }

    public static String write(Map<String, ?> values) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        StringBuilder out = new StringBuilder();
        writeTable(out, List.of(), new LinkedHashMap<>(values));
        return out.toString();
    }

    public static String writeObject(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return write(toWritableMap(value));
    }

    private static void writeTable(StringBuilder out, List<String> path, Map<String, ?> table) {
        List<Map.Entry<String, ?>> scalars = new ArrayList<>();
        List<Map.Entry<String, ?>> tables = new ArrayList<>();
        List<Map.Entry<String, ?>> arraysOfTables = new ArrayList<>();

        for (Map.Entry<String, ?> entry : table.entrySet()) {
            Object value = unwrapOptional(entry.getValue());
            if (value == null) {
                continue;
            }
            if (isArrayOfTables(value)) {
                arraysOfTables.add(Map.entry(entry.getKey(), value));
            } else if (isTable(value)) {
                tables.add(Map.entry(entry.getKey(), value));
            } else {
                scalars.add(Map.entry(entry.getKey(), value));
            }
        }

        if (!path.isEmpty()) {
            out.append('[').append(formatPath(path)).append("]\n");
        }
        for (Map.Entry<String, ?> entry : scalars) {
            out.append(formatKey(entry.getKey())).append(" = ").append(formatValue(entry.getValue())).append('\n');
        }
        if (!scalars.isEmpty() && (!tables.isEmpty() || !arraysOfTables.isEmpty())) {
            out.append('\n');
        }

        for (int index = 0; index < tables.size(); index++) {
            Map.Entry<String, ?> entry = tables.get(index);
            writeTable(out, append(path, entry.getKey()), toWritableMap(entry.getValue()));
            if (index < tables.size() - 1 || !arraysOfTables.isEmpty()) {
                out.append('\n');
            }
        }

        for (int index = 0; index < arraysOfTables.size(); index++) {
            Map.Entry<String, ?> entry = arraysOfTables.get(index);
            writeArrayOfTables(out, append(path, entry.getKey()), entry.getValue());
            if (index < arraysOfTables.size() - 1) {
                out.append('\n');
            }
        }
    }

    private static void writeArrayOfTables(StringBuilder out, List<String> path, Object value) {
        List<?> values = toList(value);
        for (int index = 0; index < values.size(); index++) {
            Object item = unwrapOptional(values.get(index));
            if (!isTable(item)) {
                throw new TomlWriteException("Array of tables '" + String.join(".", path) + "' contains a non-table value");
            }
            out.append("[[").append(formatPath(path)).append("]]\n");
            writeTableBody(out, toWritableMap(item));
            if (index < values.size() - 1) {
                out.append('\n');
            }
        }
    }

    private static void writeTableBody(StringBuilder out, Map<String, ?> table) {
        List<Map.Entry<String, ?>> scalars = new ArrayList<>();
        List<Map.Entry<String, ?>> tables = new ArrayList<>();

        for (Map.Entry<String, ?> entry : table.entrySet()) {
            Object value = unwrapOptional(entry.getValue());
            if (value == null) {
                continue;
            }
            if (isTable(value) || isArrayOfTables(value)) {
                tables.add(Map.entry(entry.getKey(), value));
            } else {
                scalars.add(Map.entry(entry.getKey(), value));
            }
        }

        for (Map.Entry<String, ?> entry : scalars) {
            out.append(formatKey(entry.getKey())).append(" = ").append(formatValue(entry.getValue())).append('\n');
        }
        for (Map.Entry<String, ?> entry : tables) {
            out.append(formatKey(entry.getKey())).append(" = ").append(formatInlineValue(entry.getValue())).append('\n');
        }
    }

    private static String formatValue(Object rawValue) {
        Object value = unwrapOptional(rawValue);
        if (value == null) {
            throw new TomlWriteException("TOML has no null value");
        }
        if (value instanceof String string) {
            return quoteString(string);
        }
        if (value instanceof Character character) {
            return quoteString(String.valueOf(character));
        }
        if (value instanceof Boolean bool) {
            return bool.toString();
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
            return value.toString();
        }
        if (value instanceof Float number) {
            return formatFloat(number.doubleValue());
        }
        if (value instanceof Double number) {
            return formatFloat(number);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Enum<?> enumValue) {
            return quoteString(enumValue.name());
        }
        if (value instanceof URI || value instanceof URL || value instanceof Duration) {
            return quoteString(value.toString());
        }
        if (value instanceof OffsetDateTime dateTime) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
        }
        if (value instanceof LocalDateTime dateTime) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
        }
        if (value instanceof LocalDate date) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
        }
        if (value instanceof LocalTime time) {
            return DateTimeFormatter.ISO_LOCAL_TIME.format(time);
        }
        if (value instanceof Instant instant) {
            return DateTimeFormatter.ISO_INSTANT.format(instant);
        }
        if (value instanceof Date date) {
            return DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
        }
        if (isArrayLike(value)) {
            return formatArray(value);
        }
        if (isTable(value)) {
            return formatInlineValue(value);
        }
        throw new TomlWriteException("Unsupported TOML write value type: " + value.getClass().getName());
    }

    private static String formatInlineValue(Object value) {
        if (isArrayOfTables(value)) {
            throw new TomlWriteException("Nested arrays of tables cannot be written inline");
        }
        if (isTable(value)) {
            Map<String, ?> table = toWritableMap(value);
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, ?> entry : table.entrySet()) {
                Object child = unwrapOptional(entry.getValue());
                if (child != null) {
                    parts.add(formatKey(entry.getKey()) + " = " + formatValue(child));
                }
            }
            return "{ " + String.join(", ", parts) + " }";
        }
        return formatValue(value);
    }

    private static String formatArray(Object value) {
        List<?> values = toList(value);
        List<String> parts = new ArrayList<>();
        for (Object item : values) {
            parts.add(formatValue(item));
        }
        return "[ " + String.join(", ", parts) + " ]";
    }

    private static String formatFloat(double value) {
        if (Double.isNaN(value)) {
            return "nan";
        }
        if (value == Double.POSITIVE_INFINITY) {
            return "inf";
        }
        if (value == Double.NEGATIVE_INFINITY) {
            return "-inf";
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String quoteString(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            switch (c) {
                case '\b' -> out.append("\\b");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\f' -> out.append("\\f");
                case '\r' -> out.append("\\r");
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                default -> {
                    if ((c >= 0x00 && c <= 0x1F) || c == 0x7F) {
                        out.append("\\u").append(String.format("%04X", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private static String formatPath(List<String> path) {
        return path.stream().map(TomlWriter::formatKey).reduce((left, right) -> left + "." + right).orElse("");
    }

    private static String formatKey(String key) {
        if (key == null || key.isEmpty()) {
            return quoteString("");
        }
        if (key.matches("[A-Za-z0-9_-]+")) {
            return key;
        }
        return quoteString(key);
    }

    private static Map<String, Object> toWritableMap(Object value) {
        Object unwrapped = unwrapOptional(value);
        if (unwrapped instanceof TomlDocument document) {
            return new LinkedHashMap<>(document.toMap());
        }
        if (unwrapped instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new TomlWriteException("TOML table keys must be strings");
                }
                result.put(key, entry.getValue());
            }
            return result;
        }
        if (unwrapped == null || isScalar(unwrapped) || isArrayLike(unwrapped)) {
            throw new TomlWriteException("Expected a table-like value but got " + (unwrapped == null ? "null" : unwrapped.getClass().getName()));
        }
        if (unwrapped.getClass() == Object.class || unwrapped.getClass().getName().startsWith("java.")) {
            throw new TomlWriteException("Unsupported TOML write value type: " + unwrapped.getClass().getName());
        }
        if (unwrapped.getClass().isRecord()) {
            return recordToMap(unwrapped);
        }
        return beanToMap(unwrapped);
    }

    private static Map<String, Object> recordToMap(Object record) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            if (component.isAnnotationPresent(TomlIgnore.class)) {
                continue;
            }
            try {
                Method accessor = component.getAccessor();
                accessor.setAccessible(true);
                String key = componentKey(component);
                result.put(key, accessor.invoke(record));
            } catch (ReflectiveOperationException exception) {
                throw new TomlWriteException("Cannot read record component " + component.getName(), exception);
            }
        }
        return result;
    }

    private static Map<String, Object> beanToMap(Object bean) {
        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> cursor = bean.getClass();
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    result.put(fieldKey(field), field.get(bean));
                } catch (IllegalAccessException exception) {
                    throw new TomlWriteException("Cannot read field " + field.getName(), exception);
                }
            }
            cursor = cursor.getSuperclass();
        }
        return result;
    }

    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers)
            || Modifier.isTransient(modifiers)
            || field.isSynthetic()
            || field.isAnnotationPresent(TomlIgnore.class);
    }

    private static String componentKey(RecordComponent component) {
        TomlSection section = component.getAnnotation(TomlSection.class);
        if (section != null && !section.value().isBlank()) {
            return section.value();
        }
        TomlKey key = component.getAnnotation(TomlKey.class);
        return key == null ? component.getName() : key.value();
    }

    private static String fieldKey(Field field) {
        TomlSection section = field.getAnnotation(TomlSection.class);
        if (section != null && !section.value().isBlank()) {
            return section.value();
        }
        TomlKey key = field.getAnnotation(TomlKey.class);
        return key == null ? field.getName() : key.value();
    }

    private static boolean isScalar(Object value) {
        Object unwrapped = unwrapOptional(value);
        return unwrapped == null
            || unwrapped instanceof String
            || unwrapped instanceof Character
            || unwrapped instanceof Boolean
            || unwrapped instanceof Number
            || unwrapped instanceof Enum<?>
            || unwrapped instanceof URI
            || unwrapped instanceof URL
            || unwrapped instanceof Duration
            || unwrapped instanceof OffsetDateTime
            || unwrapped instanceof LocalDateTime
            || unwrapped instanceof LocalDate
            || unwrapped instanceof LocalTime
            || unwrapped instanceof Instant
            || unwrapped instanceof Date;
    }

    private static boolean isTable(Object value) {
        Object unwrapped = unwrapOptional(value);
        return unwrapped != null && !isScalar(unwrapped) && !isArrayLike(unwrapped);
    }

    private static boolean isArrayOfTables(Object value) {
        if (!isArrayLike(value)) {
            return false;
        }
        List<?> values = toList(value);
        return !values.isEmpty() && values.stream()
            .map(TomlWriter::unwrapOptional)
            .allMatch(TomlWriter::isTable);
    }

    private static boolean isArrayLike(Object value) {
        Object unwrapped = unwrapOptional(value);
        return unwrapped != null && (unwrapped instanceof Collection<?> || unwrapped.getClass().isArray());
    }

    private static List<?> toList(Object value) {
        Object unwrapped = unwrapOptional(value);
        if (unwrapped instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (unwrapped != null && unwrapped.getClass().isArray()) {
            int length = Array.getLength(unwrapped);
            List<Object> values = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                values.add(Array.get(unwrapped, index));
            }
            return values;
        }
        throw new TomlWriteException("Expected an array-like value");
    }

    private static Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    private static List<String> append(List<String> path, String key) {
        List<String> next = new ArrayList<>(path);
        next.add(key);
        return next;
    }
}
