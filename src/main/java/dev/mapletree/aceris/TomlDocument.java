package dev.mapletree.aceris;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

/**
 * Immutable interpreted TOML document.
 */
public final class TomlDocument {
    private final Map<String, Object> values;

    TomlDocument(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public Map<String, Object> toMap() {
        return values;
    }

    public Optional<Object> get(String path) {
        requirePath(path);
        return getPath(values, path);
    }

    public Optional<String> getString(String path) {
        return getAs(path, String.class);
    }

    public Optional<Integer> getInteger(String path) {
        return getAs(path, Integer.class);
    }

    public Optional<Byte> getByte(String path) {
        return getAs(path, Byte.class);
    }

    public Optional<Short> getShort(String path) {
        return getAs(path, Short.class);
    }

    public Optional<Long> getLong(String path) {
        return getAs(path, Long.class);
    }

    public Optional<BigInteger> getBigInteger(String path) {
        return getAs(path, BigInteger.class);
    }

    public Optional<Float> getFloat(String path) {
        return getAs(path, Float.class);
    }

    public Optional<Double> getDouble(String path) {
        return getAs(path, Double.class);
    }

    public Optional<BigDecimal> getBigDecimal(String path) {
        return getAs(path, BigDecimal.class);
    }

    public Optional<Boolean> getBoolean(String path) {
        return getAs(path, Boolean.class);
    }

    public Optional<Character> getCharacter(String path) {
        return getAs(path, Character.class);
    }

    public Optional<URI> getUri(String path) {
        return getAs(path, URI.class);
    }

    public Optional<URL> getUrl(String path) {
        return getAs(path, URL.class);
    }

    public Optional<LocalDate> getLocalDate(String path) {
        return getAs(path, LocalDate.class);
    }

    public Optional<LocalTime> getLocalTime(String path) {
        return getAs(path, LocalTime.class);
    }

    public Optional<LocalDateTime> getLocalDateTime(String path) {
        return getAs(path, LocalDateTime.class);
    }

    public Optional<OffsetDateTime> getOffsetDateTime(String path) {
        return getAs(path, OffsetDateTime.class);
    }

    public Optional<Instant> getInstant(String path) {
        return getAs(path, Instant.class);
    }

    public Optional<Date> getDate(String path) {
        return getAs(path, Date.class);
    }

    public Optional<java.sql.Date> getSqlDate(String path) {
        return getAs(path, java.sql.Date.class);
    }

    public Optional<Timestamp> getTimestamp(String path) {
        return getAs(path, Timestamp.class);
    }

    public Optional<Duration> getDuration(String path) {
        return getAs(path, Duration.class);
    }

    public <E extends Enum<E>> Optional<E> getEnum(String path, Class<E> enumType) {
        return getAs(path, enumType);
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getTable(String path) {
        return get(path)
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(map -> (Map<String, Object>) map);
    }

    @SuppressWarnings("unchecked")
    public Optional<List<Object>> getArray(String path) {
        return get(path)
            .filter(List.class::isInstance)
            .map(List.class::cast)
            .map(list -> (List<Object>) list);
    }

    public <E> Optional<List<E>> getList(String path, Class<E> elementType) {
        requireType(elementType);
        return get(path).map(value -> TomlMapper.convertList(value, elementType));
    }

    public <E> Optional<Set<E>> getSet(String path, Class<E> elementType) {
        requireType(elementType);
        return get(path).map(value -> TomlMapper.convertSet(value, elementType));
    }

    public <E> Optional<SortedSet<E>> getSortedSet(String path, Class<E> elementType) {
        requireType(elementType);
        return get(path).map(value -> TomlMapper.convertSortedSet(value, elementType));
    }

    public <E extends Enum<E>> Optional<EnumSet<E>> getEnumSet(String path, Class<E> enumType) {
        requireType(enumType);
        return get(path).map(value -> TomlMapper.convertEnumSet(value, enumType));
    }

    public <E> Optional<E[]> getArray(String path, Class<E> elementType) {
        requireType(elementType);
        return get(path).map(value -> TomlMapper.convertArray(value, elementType));
    }

    public <T> Optional<T> getAs(String path, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return get(path).map(value -> TomlMapper.convert(value, type));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAs(String path, TomlType<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return get(path).map(value -> (T) TomlMapper.convert(value, type.type()));
    }

    public <T> T to(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return TomlMapper.convert(values, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T to(TomlType<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return (T) TomlMapper.convert(values, type.type());
    }

    private static void requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be null or blank");
        }
    }

    private static void requireType(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
    }

    private static Optional<Object> getPath(Map<String, Object> root, String path) {
        Object cursor = root;
        for (String part : path.split("\\.")) {
            if (!(cursor instanceof Map<?, ?> map) || !map.containsKey(part)) {
                return Optional.empty();
            }
            cursor = map.get(part);
        }
        return Optional.ofNullable(cursor);
    }
}
