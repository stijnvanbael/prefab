package be.appify.prefab.core.util;

public interface ServiceLocator {
    <T> T getInstance(Class<T> serviceType);
}
