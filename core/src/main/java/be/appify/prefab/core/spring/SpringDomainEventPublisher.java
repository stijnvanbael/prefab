package be.appify.prefab.core.spring;

import be.appify.prefab.core.domain.DomainEventPublisher;
import jakarta.annotation.PreDestroy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Spring-based implementation of DomainEventPublisher using ApplicationEventPublisher. */
@Component
public class SpringDomainEventPublisher extends DomainEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        DomainEventPublisher.setInstance(this);
    }

    @PreDestroy
    void unregister() {
        DomainEventPublisher.reset();
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
