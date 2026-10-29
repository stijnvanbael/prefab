package be.appify.prefab.streams.testing;

import be.appify.prefab.streams.PrefabStreams;
import java.util.ServiceLoader;

/**
 * Backend-specific test environment: declares topologies on its {@link #streams()} and runs them in-process.
 *
 * <p>Implementations are provided by a {@link Factory}, registered through {@link ServiceLoader}.
 */
public interface TopologyTestHarness {

    /**
     * The streams to declare the topology under test on.
     *
     * @return the streams
     */
    PrefabStreams streams();

    /**
     * Starts the topology that was declared on {@link #streams()} so far.
     *
     * @return the running session
     */
    TopologySession start();

    /**
     * Creates a harness for the backend that is available on the classpath.
     *
     * @param appId the application id, used to derive topic names
     * @return a new harness
     * @throws IllegalStateException when no backend is available
     */
    static TopologyTestHarness create(String appId) {
        return ServiceLoader.load(Factory.class).findFirst()
                .orElseThrow(() -> new IllegalStateException("No TopologyTestHarness.Factory found on the classpath"))
                .create(appId);
    }

    /** Creates harnesses for one backend. */
    interface Factory {
        /**
         * Creates a harness.
         *
         * @param appId the application id, used to derive topic names
         * @return a new harness
         */
        TopologyTestHarness create(String appId);
    }
}
