package be.appify.prefab.core.domain;

import be.appify.prefab.core.util.ServiceLocator;

/** Publisher for domain events */
public abstract class DomainEventPublisher {

    /** Service locator to get the instance of the publisher */
    protected static ServiceLocator serviceLocator;

    /** Constructs a new DomainEventPublisher. */
    public DomainEventPublisher() {
    }

    private static final class InstanceHolder {
        private static final DomainEventPublisher instance = serviceLocator != null
            ? serviceLocator.getInstance(DomainEventPublisher.class)
            : null;
    }

    /**
     * Get the singleton instance of the DomainEventPublisher
     * @return The singleton instance of the DomainEventPublisher
     */
    public static DomainEventPublisher getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Publish a domain event
     * @param event the event to publish
     */
    public abstract void publish(Object event);
}
