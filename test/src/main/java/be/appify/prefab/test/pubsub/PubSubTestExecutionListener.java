package be.appify.prefab.test.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.test.inmemory.TestInfrastructureMode;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Gives every test a clean messaging slate on real Pub/Sub infrastructure.
 * <p>
 * {@link be.appify.prefab.test.persistence.PersistenceTestExecutionListener} removes all rows before each test, while
 * Pub/Sub keeps subscriptions and retains unprocessed messages. Events published by an earlier test can therefore
 * arrive once a later test has emptied its tables; handlers then fail on a missing target and retry, starving
 * subsequent valid events. This listener runs first: it stops delivery, lets in-flight messages settle, discards any
 * backlog, and recreates every subscription empty before persistence is cleaned.
 * <p>
 * Only active in containers mode; the in-memory bus needs no isolation. Production processing semantics
 * (retry, dead-letter) are untouched. Registered by default via {@code META-INF/spring.factories}.
 */
public class PubSubTestExecutionListener extends AbstractTestExecutionListener {

    /** Constructs a new PubSubTestExecutionListener. */
    public PubSubTestExecutionListener() {
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 300;
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        if (!TestInfrastructureMode.containersEnabled(testContext.getApplicationContext())) {
            return;
        }
        testContext.getApplicationContext().getBeanProvider(PubSubUtil.class).ifAvailable(PubSubUtil::resetSubscriptions);
    }
}
