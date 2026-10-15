package be.appify.prefab.test.domain;

import be.appify.prefab.core.domain.DomainEventPublisher;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Binds the {@link DomainEventPublisher} of the running test's own application context before each test,
 * and restores the previously bound publisher afterwards.
 *
 * <p>Spring caches application contexts across tests, so the static {@link DomainEventPublisher} instance
 * would otherwise remain bound to whichever context was created last. Registered by default via
 * {@code META-INF/spring.factories}.
 */
public class DomainEventPublisherTestExecutionListener extends AbstractTestExecutionListener {

    private static final String PREVIOUS_PUBLISHER =
            DomainEventPublisherTestExecutionListener.class.getName() + ".previousPublisher";

    /** Constructs a new DomainEventPublisherTestExecutionListener. */
    public DomainEventPublisherTestExecutionListener() {
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        var own = testContext.getApplicationContext()
                .getBeanProvider(DomainEventPublisher.class)
                .getIfUnique();
        if (own != null) {
            testContext.setAttribute(PREVIOUS_PUBLISHER, Optional.ofNullable(DomainEventPublisher.getInstance()));
            DomainEventPublisher.setInstance(own);
        }
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        if (testContext.hasAttribute(PREVIOUS_PUBLISHER)) {
            var previous = (Optional<?>) testContext.removeAttribute(PREVIOUS_PUBLISHER);
            previous.ifPresentOrElse(
                    publisher -> DomainEventPublisher.setInstance((DomainEventPublisher) publisher),
                    DomainEventPublisher::reset);
        }
    }
}
