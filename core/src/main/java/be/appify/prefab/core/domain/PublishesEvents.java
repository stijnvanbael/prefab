package be.appify.prefab.core.domain;

public interface PublishesEvents {
    default void publish(Object event) {
        var eventPublisher = DomainEventPublisher.getInstance();
        if (eventPublisher != null) {
            eventPublisher.publish(event);
        }
    }
}
