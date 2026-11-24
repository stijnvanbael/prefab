package be.appify.prefab.test.inmemory.streams;

import be.appify.prefab.test.inmemory.TestInfrastructureMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Clears the in-memory stream state before each test instance is prepared, like the persistence listener does for the
 * aggregates, so that aggregations and replicated stores of one test do not leak into the next.
 */
public class InMemoryStreamsTestExecutionListener extends AbstractTestExecutionListener {

    /** Constructs a new InMemoryStreamsTestExecutionListener. */
    public InMemoryStreamsTestExecutionListener() {
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        var context = testContext.getApplicationContext();
        if (!TestInfrastructureMode.inMemory(context.getEnvironment())) {
            return;
        }
        context.getBeanProvider(StreamStateReset.class).ifAvailable(StreamStateReset::reset);
    }
}
