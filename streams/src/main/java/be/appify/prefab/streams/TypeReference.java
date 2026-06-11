package be.appify.prefab.streams;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeReference<T> {
    protected final Type type;

    public TypeReference() {
        this.type =((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public TypeReference(Type type) {
        this.type = type;
    }

    public Type type() { return type; }

    public static <T> TypeReference<T> of(Class<T> type) {
        return new SimpleClass<>(type);
    }

    /**
     * Creates a {@code TypeReference} whose {@link #name()} returns a human-readable
     * representative label rather than the plain class name.
     *
     * <p>The representative name is used for Kafka state-store naming so that stores get
     * descriptive, unique names. The raw type still drives {@link #rawType()},
     * {@link #equals}, and {@link #hashCode}, keeping store-lookup semantics intact.
     */
    public static <T> TypeReference<T> of(Class<T> rawType, String representativeName) {
        return new NamedClass<>(rawType, representativeName);
    }

    public String name() {
        return type.getTypeName().substring(type.getTypeName().lastIndexOf('.') + 1);
    }

    @SuppressWarnings("unchecked")
    public Class<T> rawType() {
        if (type instanceof ParameterizedType) {
            return (Class<T>) ((ParameterizedType) type).getRawType();
        } else if(type instanceof Class<?>) {
            return (Class<T>) type;
        }
        throw new IllegalArgumentException("Cannot create type reference of type " + type.getTypeName());
    }

    private static class SimpleClass<T> extends TypeReference<T> {
        public SimpleClass(Type type) {
            super(type);
        }
    }

    private static final class NamedClass<T> extends TypeReference<T> {
        private final String name;

        NamedClass(Class<T> rawType, String name) {
            super(rawType);
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeReference && type.equals(((TypeReference<?>) obj).type);
    }

    @Override
    public String toString() {
        return name();
    }
}

