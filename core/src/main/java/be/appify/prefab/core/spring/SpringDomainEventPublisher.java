package be.appify.prefab.core.spring;

import be.appify.prefab.core.domain.DomainEventDispatcher;
import be.appify.prefab.core.domain.DomainEventPublisher;
import jakarta.annotation.PreDestroy;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring-based implementation of {@link DomainEventPublisher}.
 *
 * <p>Events whose type is registered in an infrastructure registry (i.e. {@code @Event}-annotated
 * records) are dispatched directly to the matching {@link DomainEventDispatcher} beans, bypassing
 * the Spring application-event bus. All other events — those not claimed by any dispatcher —
 * fall back to {@link ApplicationEventPublisher} so that plain Spring events continue to work.
 */
@Component
public class SpringDomainEventPublisher extends DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final List<DomainEventDispatcher> dispatchers;

    SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                               List<DomainEventDispatcher> dispatchers) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.dispatchers = List.copyOf(dispatchers);
        DomainEventPublisher.setInstance(this);
    }

    @PreDestroy
    void unregister() {
        DomainEventPublisher.reset();
    }

    @Override
    public void publish(Object event) {
        var matched = dispatchers.stream()
                .filter(d -> d.canDispatch(event.getClass()))
                .toList();

        if (matched.isEmpty()) {
            applicationEventPublisher.publishEvent(event);
        } else {
            matched.forEach(d -> d.dispatch(event));
        }
    }
}
