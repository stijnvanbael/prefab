package be.appify.prefab.core.domain;

/**
 * Interface for entities that can publish domain events.
 */
public interface PublishesEvents {
    /**
     * Publishes a domain event using the DomainEventPublisher.
     *
     * @param event the event to publish
     * @deprecated Bind an {@link EventPublisher} once and publish through the typed handle.
     */
    @Deprecated(since = "0.12.2")
    default void publish(Object event) {
        publishEvent(event);
    }

    /**
     * Publishes through the active infrastructure publisher, when one is registered.
     *
     * @param event event payload
     * @deprecated Bind an {@link EventPublisher} instead.
     */
    @Deprecated(since = "0.12.2")
    static void publishEvent(Object event) {
        var eventPublisher = DomainEventPublisher.getInstance();
        if (eventPublisher != null) {
            eventPublisher.publish(event);
        }
    }
}
