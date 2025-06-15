package be.appify.prefab.core.domain;

import be.appify.prefab.core.util.ServiceLocator;

public abstract class DomainEventPublisher {

    protected static ServiceLocator serviceLocator;

    private static final class InstanceHolder {
        private static final DomainEventPublisher instance = serviceLocator != null
            ? serviceLocator.getInstance(DomainEventPublisher.class)
            : null;
    }

    public static DomainEventPublisher getInstance() {
        return InstanceHolder.instance;
    }

    public abstract void publish(Object event);
}
