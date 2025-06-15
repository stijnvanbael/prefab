package be.appify.prefab.core.service;

/**
 * A reference to another aggregate root.
 *
 * @param <T>
 *     the type of the referenced aggregate root
 */
public interface Reference<T> {
    /**
     * @return the ID of the referenced aggregate root
     */
    String id();

    /**
     * Checks whether the referenced aggregate root exists.
     *
     * @return true if the referenced aggregate root exists, false otherwise
     */
    boolean exists();

    /**
     * Resolves the reference to the other aggregate root. Any changes to the returned object will not be persisted.
     *
     * @return a read-only version of the referenced aggregate root
     */
    T resolveReadOnly();
}
