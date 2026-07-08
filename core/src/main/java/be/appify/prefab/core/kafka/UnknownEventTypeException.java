package be.appify.prefab.core.kafka;

/**
 * Thrown when a message arrives on a subscribed topic carrying an event type that is not
 * registered in the local {@link EventRegistry}.
 *
 * <p>This exception signals that the message should be silently acknowledged and processing
 * of subsequent messages should continue uninterrupted. It is not a transient failure and
 * must not trigger retries or dead-letter routing.
 */
public class UnknownEventTypeException extends RuntimeException {

    private final String eventTypeName;

    /**
     * Constructs an UnknownEventTypeException for the given event type name.
     *
     * @param eventTypeName the fully-qualified or simple name of the unrecognised event type
     */
    public UnknownEventTypeException(String eventTypeName) {
        super("Unknown event type: " + eventTypeName);
        this.eventTypeName = eventTypeName;
    }

    /**
     * Returns the name of the event type that could not be resolved.
     *
     * @return the event type name
     */
    public String eventTypeName() {
        return eventTypeName;
    }
}

