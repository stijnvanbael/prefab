package be.appify.prefab.core.spring;

import be.appify.prefab.core.domain.DomainEventPublisher;
import be.appify.prefab.core.outbox.PendingEventBuffer;
import be.appify.prefab.core.util.ServiceLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Spring-based implementation of DomainEventPublisher using ApplicationEventPublisher. */
@Component
public class SpringDomainEventPublisher extends DomainEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    SpringDomainEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            ServiceLocator serviceLocator
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        DomainEventPublisher.serviceLocator = serviceLocator;
    }

    @Override
    public void publish(Object event) {
        PendingEventBuffer.add(event);
    }

    /**
     * Returns the underlying {@link ApplicationEventPublisher} so that outbox infrastructure
     * (e.g. JDBC/MongoDB templates and the relay service) can publish events directly.
     *
     * @return the Spring application event publisher
     * @throws IllegalStateException if the registered {@link DomainEventPublisher} is not a
     *                               {@link SpringDomainEventPublisher}
     */
    public static ApplicationEventPublisher getApplicationEventPublisher() {
        DomainEventPublisher instance = getInstance();
        if (!(instance instanceof SpringDomainEventPublisher publisher)) {
            throw new IllegalStateException(
                    "Expected a SpringDomainEventPublisher but found: "
                            + (instance == null ? "null" : instance.getClass().getName()));
        }
        return publisher.applicationEventPublisher;
    }
}
