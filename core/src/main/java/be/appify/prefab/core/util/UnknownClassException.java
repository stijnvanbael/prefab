package be.appify.prefab.core.util;

/** Thrown when a class cannot be loaded by its name. */
public class UnknownClassException extends IllegalArgumentException {
    private final String className;

    /**
     * Constructs a new UnknownClassException.
     *
     * @param className
     *         the fully qualified name of the class that was not found
     * @param cause
     *         the underlying class loading failure
     */
    public UnknownClassException(String className, ClassNotFoundException cause) {
        super("Class [%s] not found".formatted(className), cause);
        this.className = className;
    }

    /**
     * The name of the class that was not found.
     *
     * @return the fully qualified class name
     */
    public String className() {
        return className;
    }
}
