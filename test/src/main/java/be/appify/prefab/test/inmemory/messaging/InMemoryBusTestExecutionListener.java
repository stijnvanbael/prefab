package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.test.inmemory.TestInfrastructureMode;
import java.util.List;
import java.util.Optional;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Resets the {@link InMemoryEventBus} before each test and fails the test when an event handler threw during it.
 * <p>
 * Handler exceptions never reach the publisher, so without this listener a broken consumer would go unnoticed.
 * A test that expects a handler to fail clears the failure with {@link InMemoryEventBus#drainFailures()}.
 */
public class InMemoryBusTestExecutionListener extends AbstractTestExecutionListener {

    /** Constructs a new InMemoryBusTestExecutionListener. */
    public InMemoryBusTestExecutionListener() {
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        bus(testContext).ifPresent(InMemoryEventBus::reset);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        bus(testContext).ifPresent(bus -> failIfHandlersFailed(bus.drainFailures()));
    }

    private static void failIfHandlersFailed(List<HandlerFailure> failures) {
        if (failures.isEmpty()) {
            return;
        }
        var error = new AssertionError("%d event handler(s) failed during the test: %s".formatted(
                failures.size(), describe(failures)), failures.getFirst().cause());
        failures.stream().skip(1).forEach(failure -> error.addSuppressed(failure.cause()));
        throw error;
    }

    private static String describe(List<HandlerFailure> failures) {
        return failures.stream()
                .map(failure -> "%s on [%s]: %s".formatted(failure.subscriber(), failure.topic(), failure.cause()))
                .toList()
                .toString();
    }

    private static Optional<InMemoryEventBus> bus(TestContext testContext) {
        if (!TestInfrastructureMode.inMemory(testContext.getApplicationContext().getEnvironment())) {
            return Optional.empty();
        }
        return Optional.ofNullable(testContext.getApplicationContext().getBeanProvider(InMemoryEventBus.class)
                .getIfAvailable());
    }
}
