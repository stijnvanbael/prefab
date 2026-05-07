package be.appify.prefab.core.domain;

/** Publisher for domain events. */
public abstract class DomainEventPublisher {

    private static volatile DomainEventPublisher instance;

    /** Constructs a new DomainEventPublisher. */
    public DomainEventPublisher() {
    }

    /**
     * Registers the active publisher instance. Called by framework infrastructure on startup.
     *
     * @param publisher the publisher to register
     */
    public static void setInstance(DomainEventPublisher publisher) {
        instance = publisher;
    }

    /**
     * Clears the active publisher instance. Called by framework infrastructure on shutdown
     * and by test infrastructure after each test.
     */
    public static void reset() {
        instance = null;
    }

    /**
     * Returns the active publisher instance, or {@code null} when no publisher is registered.
     *
     * @return the active publisher, or {@code null}
     */
    public static DomainEventPublisher getInstance() {
        return instance;
    }

    /**
     * Publishes a domain event.
     *
     * @param event the event to publish
     */
    public abstract void publish(Object event);
}
