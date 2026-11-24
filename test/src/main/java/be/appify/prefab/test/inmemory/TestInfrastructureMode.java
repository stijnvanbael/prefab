package be.appify.prefab.test.inmemory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Decides whether integration tests run against real infrastructure (Testcontainers) or in memory.
 * <p>
 * The mode is controlled by the {@value #PROPERTY} property, which {@code @IntegrationTest(containers = true)} forces
 * to {@code true} for a single test class.
 */
public final class TestInfrastructureMode {

    /** Name of the property that switches between containers and in-memory infrastructure. */
    public static final String PROPERTY = "prefab.test.containers";

    /** Value used when {@value #PROPERTY} is not configured. */
    static final boolean DEFAULT_CONTAINERS = false;

    private TestInfrastructureMode() {
    }

    /**
     * Whether tests must run on real infrastructure.
     *
     * @param environment the environment to read the property from
     * @return {@code true} for containers mode, {@code false} for in-memory mode
     */
    public static boolean containersEnabled(Environment environment) {
        return environment.getProperty(PROPERTY, Boolean.class, DEFAULT_CONTAINERS);
    }

    /**
     * Whether tests must run on real infrastructure.
     *
     * @param context the application context of the running test
     * @return {@code true} for containers mode, {@code false} for in-memory mode
     */
    public static boolean containersEnabled(ApplicationContext context) {
        return containersEnabled(context.getEnvironment());
    }

    /**
     * Whether tests run in memory.
     *
     * @param environment the environment to read the property from
     * @return {@code true} for in-memory mode
     */
    public static boolean inMemory(Environment environment) {
        return !containersEnabled(environment);
    }
}
