package be.appify.prefab.core.domain;

/** Interface for entities that can publish domain events. */
public interface PublishesEvents {
    /**
     * Publishes a domain event using the DomainEventPublisher.
     *
     * @param event the event to publish
     */
    default void publish(Object event) {
        var eventPublisher = DomainEventPublisher.getInstance();
        if (eventPublisher != null) {
            eventPublisher.publish(event);
        }
    }
}
