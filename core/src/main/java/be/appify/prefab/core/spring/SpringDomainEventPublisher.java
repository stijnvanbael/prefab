package be.appify.prefab.core.spring;

import be.appify.prefab.core.domain.DomainEventPublisher;
import be.appify.prefab.core.util.ServiceLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDomainEventPublisher extends DomainEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            ServiceLocator serviceLocator
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        DomainEventPublisher.serviceLocator = serviceLocator;
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
