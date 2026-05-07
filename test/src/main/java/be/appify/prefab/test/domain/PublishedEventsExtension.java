package be.appify.prefab.test.domain;

import be.appify.prefab.core.domain.DomainEventPublisher;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that installs a {@link CapturingDomainEventPublisher} before each test
 * and resets the {@link DomainEventPublisher} after each test.
 *
 * <p>Enables unit testing of aggregate roots that publish domain events without a Spring context,
 * and without polluting the static publisher state for subsequent integration tests.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(PublishedEventsExtension.class)
 * class MyAggregateTest {
 *
 *     @Test
 *     void orderPlaced_publishesOrderCreatedEvent(CapturingDomainEventPublisher publisher) {
 *         var order = new Order(...);
 *         assertThat(publisher.publishedEventsOf(OrderCreated.class)).hasSize(1);
 *     }
 * }
 * }</pre>
 */
public class PublishedEventsExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(PublishedEventsExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        var publisher = new CapturingDomainEventPublisher();
        context.getStore(NAMESPACE).put(CapturingDomainEventPublisher.class, publisher);
        DomainEventPublisher.setInstance(publisher);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        DomainEventPublisher.reset();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(CapturingDomainEventPublisher.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(CapturingDomainEventPublisher.class);
    }
}

