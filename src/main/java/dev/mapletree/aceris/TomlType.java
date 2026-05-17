package dev.mapletree.aceris;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Captures nested generic target types despite Java type erasure.
 *
 * <pre>{@code
 * List<Server> servers = document.getAs("servers", new TomlType<List<Server>>() {}).orElseThrow();
 * }</pre>
 */
public abstract class TomlType<T> {
    private final Type type;

    protected TomlType() {
        Type parent = getClass().getGenericSuperclass();
        if (!(parent instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException("TomlType must be created with an anonymous generic subclass");
        }
        this.type = parameterizedType.getActualTypeArguments()[0];
    }

    final Type type() {
        return type;
    }
}
