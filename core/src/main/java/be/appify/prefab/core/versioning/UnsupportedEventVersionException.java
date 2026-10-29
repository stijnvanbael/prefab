package be.appify.prefab.core.versioning;

/** Thrown when a message carries a version that the consumer cannot read. The message is routed to the DLT. */
public class UnsupportedEventVersionException extends RuntimeException {

    /**
     * Constructs the exception.
     *
     * @param message a description of why the version is not supported
     */
    public UnsupportedEventVersionException(String message) {
        super(message);
    }
}
