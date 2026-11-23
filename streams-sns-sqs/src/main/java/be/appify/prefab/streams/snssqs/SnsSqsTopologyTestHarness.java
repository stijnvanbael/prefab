package be.appify.prefab.streams.snssqs;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.engine.InMemoryTopologyTestHarness;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestHarness;

/**
 * Topology tests for the SNS/SQS backend. They run the engine of the backend synchronously, with an in-memory source,
 * sink and state repository, so they need neither LocalStack nor a database.
 */
public final class SnsSqsTopologyTestHarness implements TopologyTestHarness {
    private final InMemoryTopologyTestHarness delegate;

    /**
     * Creates a harness for one test topology.
     *
     * @param appId application id, which prefixes the derived topic names
     * @param tracing whether message and correlation IDs are tracked
     */
    public SnsSqsTopologyTestHarness(String appId, boolean tracing) {
        delegate = new InMemoryTopologyTestHarness(appId, tracing);
    }

    @Override
    public PrefabStreams streams() {
        return delegate.streams();
    }

    @Override
    public TopologySession start() {
        return delegate.start();
    }

    /** Registers the harness for backend {@code sns-sqs}. */
    public static final class Factory implements TopologyTestHarness.Factory {
        @Override
        public String backendName() {
            return SnsSqsStreamBackend.SNS_SQS.name();
        }

        @Override
        public TopologyTestHarness create(String appId, boolean tracing) {
            return new SnsSqsTopologyTestHarness(appId, tracing);
        }
    }
}
