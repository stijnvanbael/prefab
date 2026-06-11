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

    public String name() {
        return type.getTypeName();
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
        return type.getTypeName();
    }
}

