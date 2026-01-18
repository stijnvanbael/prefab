package be.appify.prefab.core.util;

/**
 * Utility class for operations related to Java classes.
 */
public class Classes {
    private Classes() {
    }

    /**
     * Retrieves the {@link Class} object associated with the class or interface
     * with the given string name.
     *
     * @param name the fully qualified name of the desired class or interface
     * @return the {@link Class} object for the class or interface with the specified name
     * @throws IllegalArgumentException if the class with the specified name cannot be found
     */
    public static Class<?> classWithName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
