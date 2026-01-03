package be.appify.prefab.core.util;

/** Service locator interface for retrieving service instances. */
public interface ServiceLocator {
    /**
     * Gets an instance of the specified service type.
     *
     * @param serviceType the class type of the service
     * @param <T>         the type of the service
     * @return an instance of the specified service type
     */
    <T> T getInstance(Class<T> serviceType);
}
