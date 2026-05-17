package dev.mapletree.aceris;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

final class TomlMapper {
    private TomlMapper() {
    }

    static <T> T convert(Object value, Class<T> targetType) {
        return cast(convert(value, (Type) targetType), targetType);
    }

    static Object convert(Object value, Type targetType) {
        if (targetType instanceof Class<?> targetClass) {
            return convertToClass(value, targetClass);
        }
        if (targetType instanceof ParameterizedType parameterizedType) {
            return convertParameterized(value, parameterizedType);
        }
        if (targetType instanceof GenericArrayType arrayType) {
            Type componentType = arrayType.getGenericComponentType();
            if (!(value instanceof List<?> list)) {
                throw conversionError(value, arrayType);
            }
            Class<?> componentClass = rawClass(componentType);
            Object array = Array.newInstance(componentClass, list.size());
            for (int index = 0; index < list.size(); index++) {
                try {
                    Array.set(array, index, convert(list.get(index), componentType));
                } catch (IllegalArgumentException exception) {
                    throw conversionError(list.get(index), arrayType, exception);
                }
            }
            return array;
        }
        throw new TomlConversionException("Unsupported target type " + targetType.getTypeName());
    }

    @SuppressWarnings("unchecked")
    static <E> List<E> convertList(Object value, Class<E> elementType) {
        return cast(convert(value, listType(elementType)), List.class);
    }

    @SuppressWarnings("unchecked")
    static <E> Set<E> convertSet(Object value, Class<E> elementType) {
        return cast(convert(value, setType(elementType)), Set.class);
    }

    @SuppressWarnings("unchecked")
    static <E> SortedSet<E> convertSortedSet(Object value, Class<E> elementType) {
        return cast(convert(value, sortedSetType(elementType)), SortedSet.class);
    }

    @SuppressWarnings("unchecked")
    static <E extends Enum<E>> EnumSet<E> convertEnumSet(Object value, Class<E> enumType) {
        return cast(convert(value, enumSetType(enumType)), EnumSet.class);
    }

    @SuppressWarnings("unchecked")
    static <E> E[] convertArray(Object value, Class<E> elementType) {
        return (E[]) convertArrayToComponent(value, elementType);
    }

    private static Object convertToClass(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        Class<?> boxedType = box(targetType);
        if (boxedType.isInstance(value)) {
            return value;
        }
        if (boxedType == String.class) {
            return String.valueOf(value);
        }
        if (boxedType == Byte.class) {
            return toByte(value, targetType);
        }
        if (boxedType == Short.class) {
            return toShort(value, targetType);
        }
        if (boxedType == Integer.class) {
            return toInteger(value, targetType);
        }
        if (boxedType == Long.class) {
            return toLong(value, targetType);
        }
        if (boxedType == BigInteger.class) {
            return toBigInteger(value, targetType);
        }
        if (boxedType == Float.class) {
            return toBigDecimal(value, targetType).floatValue();
        }
        if (boxedType == Double.class) {
            return toBigDecimal(value, targetType).doubleValue();
        }
        if (boxedType == BigDecimal.class) {
            return toBigDecimal(value, targetType);
        }
        if (boxedType == Boolean.class) {
            return toBoolean(value, targetType);
        }
        if (boxedType == Character.class) {
            return toCharacter(value);
        }
        if (boxedType == URI.class) {
            try {
                return URI.create(requireString(value, targetType));
            } catch (IllegalArgumentException exception) {
                throw conversionError(value, targetType, exception);
            }
        }
        if (boxedType == URL.class) {
            try {
                return URI.create(requireString(value, targetType)).toURL();
            } catch (Exception exception) {
                throw conversionError(value, targetType, exception);
            }
        }
        if (boxedType == Instant.class) {
            return toInstant(value, targetType);
        }
        if (boxedType == Date.class) {
            try {
                return Date.from(toInstant(value, targetType));
            } catch (IllegalArgumentException exception) {
                throw conversionError(value, targetType, exception);
            }
        }
        if (boxedType == java.sql.Date.class) {
            if (value instanceof LocalDate localDate) {
                return java.sql.Date.valueOf(localDate);
            }
            try {
                return new java.sql.Date(Date.from(toInstant(value, targetType)).getTime());
            } catch (IllegalArgumentException exception) {
                throw conversionError(value, targetType, exception);
            }
        }
        if (boxedType == Timestamp.class) {
            try {
                return Timestamp.from(toInstant(value, targetType));
            } catch (IllegalArgumentException exception) {
                throw conversionError(value, targetType, exception);
            }
        }
        if (boxedType == Duration.class) {
            try {
                return Duration.parse(requireString(value, targetType));
            } catch (RuntimeException exception) {
                throw conversionError(value, targetType, exception);
            }
        }
        if (targetType.isEnum()) {
            return convertEnum(value, targetType);
        }
        if (targetType.isArray()) {
            return convertArrayToComponent(value, targetType.getComponentType());
        }
        if (Map.class.isAssignableFrom(targetType)) {
            if (value instanceof Map<?, ?> map) {
                return map;
            }
            throw conversionError(value, targetType);
        }
        if (List.class.isAssignableFrom(targetType)) {
            if (value instanceof List<?> list) {
                return list;
            }
            throw conversionError(value, targetType);
        }
        if (SortedSet.class.isAssignableFrom(targetType)) {
            if (value instanceof List<?> list) {
                try {
                    return new TreeSet<>(list);
                } catch (RuntimeException exception) {
                    throw conversionError(value, targetType, exception);
                }
            }
            throw conversionError(value, targetType);
        }
        if (Set.class.isAssignableFrom(targetType)) {
            if (value instanceof List<?> list) {
                return new LinkedHashSet<>(list);
            }
            throw conversionError(value, targetType);
        }
        if (value instanceof Map<?, ?> map) {
            return mapToObject(map, targetType);
        }
        throw conversionError(value, targetType);
    }

    private static Object convertParameterized(Object value, ParameterizedType targetType) {
        Type rawType = targetType.getRawType();
        if (!(rawType instanceof Class<?> rawClass)) {
            throw new TomlConversionException("Unsupported generic raw type " + rawType.getTypeName());
        }
        Type[] arguments = targetType.getActualTypeArguments();

        if (Optional.class.isAssignableFrom(rawClass)) {
            return Optional.ofNullable(value == null ? null : convert(value, arguments[0]));
        }
        if (List.class.isAssignableFrom(rawClass)) {
            return convertCollection(value, arguments[0], new ArrayList<>());
        }
        if (EnumSet.class.isAssignableFrom(rawClass)) {
            Class<?> enumClass = rawClass(arguments[0]);
            if (!enumClass.isEnum()) {
                throw new TomlConversionException("EnumSet element type must be an enum");
            }
            return convertEnumSetCollection(value, enumClass);
        }
        if (SortedSet.class.isAssignableFrom(rawClass)) {
            return convertCollection(value, arguments[0], new TreeSet<>());
        }
        if (Set.class.isAssignableFrom(rawClass)) {
            return convertCollection(value, arguments[0], new LinkedHashSet<>());
        }
        if (Map.class.isAssignableFrom(rawClass)) {
            return convertMap(value, arguments[0], arguments[1]);
        }
        return convertToClass(value, rawClass);
    }

    private static Collection<Object> convertCollection(Object value, Type elementType, Collection<Object> target) {
        if (!(value instanceof List<?> list)) {
            throw conversionError(value, List.class);
        }
        for (Object item : list) {
            try {
                target.add(convert(item, elementType));
            } catch (RuntimeException exception) {
                if (exception instanceof TomlConversionException tomlConversionException) {
                    throw tomlConversionException;
                }
                throw conversionError(item, elementType, exception);
            }
        }
        return target;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EnumSet<?> convertEnumSetCollection(Object value, Class<?> enumClass) {
        if (!(value instanceof List<?> list)) {
            throw conversionError(value, EnumSet.class);
        }
        EnumSet result = EnumSet.noneOf(enumClass.asSubclass(Enum.class));
        for (Object item : list) {
            result.add(convert(item, enumClass));
        }
        return result;
    }

    private static Map<Object, Object> convertMap(Object value, Type keyType, Type valueType) {
        if (!(value instanceof Map<?, ?> map)) {
            throw conversionError(value, Map.class);
        }
        if (rawClass(keyType) != String.class) {
            throw new TomlConversionException("TOML table mappings only support String keys");
        }
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(entry.getKey(), convert(entry.getValue(), valueType));
        }
        return result;
    }

    private static Object convertArrayToComponent(Object value, Class<?> componentType) {
        if (!(value instanceof List<?> list)) {
            throw conversionError(value, componentType.arrayType());
        }
        Object array = Array.newInstance(componentType, list.size());
        for (int index = 0; index < list.size(); index++) {
            try {
                Array.set(array, index, convert(list.get(index), componentType));
            } catch (IllegalArgumentException exception) {
                throw conversionError(list.get(index), componentType, exception);
            }
        }
        return array;
    }

    private static Object mapToObject(Map<?, ?> map, Class<?> targetType) {
        if (targetType.isRecord()) {
            return mapToRecord(map, targetType);
        }
        return mapToBeanOrConstructor(map, targetType);
    }

    private static Object mapToRecord(Map<?, ?> map, Class<?> targetType) {
        try {
            RecordComponent[] components = targetType.getRecordComponents();
            Class<?>[] parameterTypes = new Class<?>[components.length];
            Object[] arguments = new Object[components.length];

            for (int index = 0; index < components.length; index++) {
                RecordComponent component = components[index];
                parameterTypes[index] = component.getType();
                ResolvedValue resolved = resolveValue(map, component.getName(), explicitName(component.getAnnotation(TomlKey.class), component.getAnnotation(TomlSection.class)), component.getAnnotation(TomlDefault.class));
                if (!resolved.found()) {
                    throw new TomlConversionException("Missing TOML key '" + component.getName() + "' for " + targetType.getName());
                }
                arguments[index] = convert(resolved.value(), component.getGenericType());
            }

            Constructor<?> constructor = targetType.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (TomlConversionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new TomlConversionException("Cannot map TOML table to " + targetType.getName(), exception);
        }
    }

    private static Object mapToBeanOrConstructor(Map<?, ?> map, Class<?> targetType) {
        try {
            Constructor<?> constructor = targetType.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            applyBeanProperties(map, targetType, instance);
            applyFieldDefaults(map, targetType, instance);
            return instance;
        } catch (NoSuchMethodException ignored) {
            return mapToConstructor(map, targetType);
        } catch (TomlConversionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new TomlConversionException("Cannot map TOML table to " + targetType.getName(), exception);
        }
    }

    private static Object mapToConstructor(Map<?, ?> map, Class<?> targetType) {
        Constructor<?>[] constructors = targetType.getDeclaredConstructors();
        TomlConversionException lastFailure = null;
        for (Constructor<?> constructor : constructors) {
            try {
                Parameter[] parameters = constructor.getParameters();
                Object[] arguments = new Object[parameters.length];
                for (int index = 0; index < parameters.length; index++) {
                    Parameter parameter = parameters[index];
                    String name = parameter.isNamePresent() ? parameter.getName() : "";
                    ResolvedValue resolved = resolveValue(map, name, explicitName(parameter.getAnnotation(TomlKey.class), null), parameter.getAnnotation(TomlDefault.class));
                    if (!resolved.found()) {
                        throw new TomlConversionException("Missing TOML key for constructor parameter " + index + " on " + targetType.getName());
                    }
                    arguments[index] = convert(resolved.value(), parameter.getParameterizedType());
                }
                constructor.setAccessible(true);
                return constructor.newInstance(arguments);
            } catch (TomlConversionException exception) {
                lastFailure = exception;
            } catch (Exception exception) {
                lastFailure = new TomlConversionException("Cannot map TOML table to " + targetType.getName(), exception);
            }
        }
        throw lastFailure == null
            ? new TomlConversionException("No usable constructor for " + targetType.getName())
            : lastFailure;
    }

    private static void applyBeanProperties(Map<?, ?> map, Class<?> targetType, Object instance) throws Exception {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            if (setByField(instance, targetType, key, entry.getValue())) {
                continue;
            }
            if (setByMethod(instance, targetType, key, entry.getValue())) {
                continue;
            }
            if (isJavaIdentifier(key)) {
                throw new TomlConversionException("No writable property '" + key + "' on " + targetType.getName());
            }
        }
    }

    private static void applyFieldDefaults(Map<?, ?> map, Class<?> targetType, Object instance) throws IllegalAccessException {
        Class<?> cursor = targetType;
        while (cursor != null) {
            for (Field field : cursor.getDeclaredFields()) {
                TomlDefault defaultValue = field.getAnnotation(TomlDefault.class);
                if (defaultValue == null) {
                    continue;
                }
                ResolvedValue resolved = resolveValue(map, field.getName(), explicitName(field.getAnnotation(TomlKey.class), field.getAnnotation(TomlSection.class)), null);
                if (resolved.found()) {
                    continue;
                }
                field.setAccessible(true);
                field.set(instance, convert(defaultValue.value(), field.getGenericType()));
            }
            cursor = cursor.getSuperclass();
        }
    }

    private static boolean setByField(Object instance, Class<?> targetType, String key, Object value) throws IllegalAccessException {
        Class<?> cursor = targetType;
        while (cursor != null) {
            for (Field field : cursor.getDeclaredFields()) {
                if (matchesKey(key, field.getName(), explicitName(field.getAnnotation(TomlKey.class), field.getAnnotation(TomlSection.class)))) {
                    field.setAccessible(true);
                    field.set(instance, convert(value, field.getGenericType()));
                    return true;
                }
            }
            cursor = cursor.getSuperclass();
        }
        return false;
    }

    private static boolean setByMethod(Object instance, Class<?> targetType, String key, Object value) throws Exception {
        for (Method method : targetType.getMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            String propertyName = setterPropertyName(method);
            if (propertyName != null && matchesKey(key, propertyName, explicitName(method.getAnnotation(TomlKey.class), method.getAnnotation(TomlSection.class)))) {
                method.setAccessible(true);
                method.invoke(instance, convert(value, method.getGenericParameterTypes()[0]));
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertEnum(Object value, Class<?> targetType) {
        String name = requireString(value, targetType);
        try {
            return Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException exception) {
            throw conversionError(value, targetType, exception);
        }
    }

    private static BigInteger toBigInteger(Object value, Type targetType) {
        try {
            if (value instanceof BigInteger bigInteger) {
                return bigInteger;
            }
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                return BigInteger.valueOf(((Number) value).longValue());
            }
            if (value instanceof String string) {
                return new BigInteger(string);
            }
        } catch (ArithmeticException | NumberFormatException exception) {
            throw conversionError(value, targetType, exception);
        }
        throw conversionError(value, targetType);
    }

    private static byte toByte(Object value, Type targetType) {
        try {
            return toBigInteger(value, targetType).byteValueExact();
        } catch (ArithmeticException exception) {
            throw conversionError(value, targetType, exception);
        }
    }

    private static short toShort(Object value, Type targetType) {
        try {
            return toBigInteger(value, targetType).shortValueExact();
        } catch (ArithmeticException exception) {
            throw conversionError(value, targetType, exception);
        }
    }

    private static int toInteger(Object value, Type targetType) {
        try {
            return toBigInteger(value, targetType).intValueExact();
        } catch (ArithmeticException exception) {
            throw conversionError(value, targetType, exception);
        }
    }

    private static long toLong(Object value, Type targetType) {
        try {
            return toBigInteger(value, targetType).longValueExact();
        } catch (ArithmeticException exception) {
            throw conversionError(value, targetType, exception);
        }
    }

    private static BigDecimal toBigDecimal(Object value, Type targetType) {
        try {
            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal;
            }
            if (value instanceof BigInteger bigInteger) {
                return new BigDecimal(bigInteger);
            }
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                return BigDecimal.valueOf(((Number) value).longValue());
            }
            if (value instanceof Float || value instanceof Double) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            if (value instanceof String string) {
                return new BigDecimal(string);
            }
        } catch (ArithmeticException | NumberFormatException exception) {
            throw conversionError(value, targetType, exception);
        }
        throw conversionError(value, targetType);
    }

    private static Boolean toBoolean(Object value, Type targetType) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string && (string.equals("true") || string.equals("false"))) {
            return Boolean.valueOf(string);
        }
        throw conversionError(value, targetType);
    }

    private static Instant toInstant(Object value, Type targetType) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof String string) {
            try {
                return Instant.parse(string);
            } catch (RuntimeException exception) {
                throw conversionError(value, targetType, exception);
            }
        }
        throw conversionError(value, targetType);
    }

    private static Character toCharacter(Object value) {
        if (value instanceof Character character) {
            return character;
        }
        if (value instanceof String string && string.length() == 1) {
            return string.charAt(0);
        }
        throw conversionError(value, Character.class);
    }

    private static String requireString(Object value, Type targetType) {
        if (value instanceof String string) {
            return string;
        }
        throw conversionError(value, targetType);
    }

    private static ResolvedValue resolveValue(Map<?, ?> map, String javaName, String explicitKey, TomlDefault defaultValue) {
        if (explicitKey != null && map.containsKey(explicitKey)) {
            return new ResolvedValue(true, map.get(explicitKey));
        }
        if (javaName != null && !javaName.isBlank()) {
            for (Object key : map.keySet()) {
                if (key instanceof String stringKey && matchesKey(stringKey, javaName, null)) {
                    return new ResolvedValue(true, map.get(stringKey));
                }
            }
        }
        if (defaultValue != null) {
            return new ResolvedValue(true, defaultValue.value());
        }
        return new ResolvedValue(false, null);
    }

    private static boolean matchesKey(String tomlKey, String javaName, String explicitKey) {
        if (explicitKey != null) {
            return tomlKey.equals(explicitKey);
        }
        return tomlKey.equals(javaName) || normalizeKey(tomlKey).equals(javaName);
    }

    private static String explicitName(TomlKey tomlKey, TomlSection tomlSection) {
        if (tomlKey != null) {
            return tomlKey.value();
        }
        if (tomlSection != null && !tomlSection.value().isBlank()) {
            return tomlSection.value();
        }
        return null;
    }

    private static String normalizeKey(String key) {
        StringBuilder result = new StringBuilder();
        boolean uppercaseNext = false;
        for (int index = 0; index < key.length(); index++) {
            char c = key.charAt(index);
            if (c == '-' || c == '_' || Character.isWhitespace(c)) {
                uppercaseNext = result.length() > 0;
                continue;
            }
            if (uppercaseNext) {
                result.append(Character.toUpperCase(c));
                uppercaseNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String setterPropertyName(Method method) {
        String name = method.getName();
        if (!name.startsWith("set") || name.length() <= 3) {
            return null;
        }
        return Character.toLowerCase(name.charAt(3)) + name.substring(4);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value, Class<T> targetType) {
        return (T) value;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?> clazz) {
            return clazz;
        }
        throw new TomlConversionException("Unsupported target type " + type.getTypeName());
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private static boolean isJavaIdentifier(String value) {
        if (value.isEmpty() || !Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
            if (!Character.isJavaIdentifierPart(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static ParameterizedType listType(Type elementType) {
        return parameterizedType(List.class, elementType);
    }

    private static ParameterizedType setType(Type elementType) {
        return parameterizedType(Set.class, elementType);
    }

    private static ParameterizedType sortedSetType(Type elementType) {
        return parameterizedType(SortedSet.class, elementType);
    }

    private static ParameterizedType enumSetType(Type elementType) {
        return parameterizedType(EnumSet.class, elementType);
    }

    private static ParameterizedType parameterizedType(Class<?> rawType, Type... arguments) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return arguments.clone();
            }

            @Override
            public Type getRawType() {
                return rawType;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }

    private static TomlConversionException conversionError(Object value, Type targetType) {
        return new TomlConversionException("Cannot convert " + describe(value) + " to " + targetType.getTypeName());
    }

    private static TomlConversionException conversionError(Object value, Type targetType, Throwable cause) {
        return new TomlConversionException("Cannot convert " + describe(value) + " to " + targetType.getTypeName(), cause);
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        return "'" + value + "' (" + value.getClass().getName() + ")";
    }

    private record ResolvedValue(boolean found, Object value) {
    }
}
