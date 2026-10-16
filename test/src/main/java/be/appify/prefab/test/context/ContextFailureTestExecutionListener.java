package be.appify.prefab.test.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Makes application context load failures easy to diagnose.
 *
 * <p>Spring caches a failed context load and fails every later test sharing that context with a generic
 * "failure threshold exceeded" error, hiding the real cause. This listener remembers the original failure
 * per context configuration and, for each affected test class, logs the original cause again and
 * attaches it as the cause of the failure.
 * Registered by default via {@code META-INF/spring.factories}.
 */
public class ContextFailureTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ContextFailureTestExecutionListener.class);
    private static final Map<MergedContextConfiguration, Throwable> FAILURES = new ConcurrentHashMap<>();

    /** Constructs a new ContextFailureTestExecutionListener. */
    public ContextFailureTestExecutionListener() {
    }

    @Override
    public int getOrder() {
        return 1999;
    }

    @Override
    public void prepareTestInstance(TestContext testContext) {
        try {
            testContext.getApplicationContext();
        } catch (RuntimeException failure) {
            handle(testContext.getTestClass(), failure);
        }
    }

    private static void handle(Class<?> testClass, RuntimeException failure) {
        var key = BootstrapUtils.resolveTestContextBootstrapper(testClass).buildMergedContextConfiguration();
        var original = FAILURES.putIfAbsent(key, failure);
        if (original == null) {
            log.error("Failed to load application context for {}", testClass.getName(), failure);
            throw failure;
        }
        log.error("Application context for {} failed to load, repeating the original failure",
                testClass.getName(), original);
        throw new IllegalStateException(
                "Application context for %s failed to load: %s".formatted(testClass.getName(), original.getMessage()),
                original);
    }
}
