package be.appify.prefab.streams.pubsub;

import be.appify.prefab.streams.engine.InMemoryTopologyTestHarness;
import be.appify.prefab.streams.testing.TopologyTestHarness;

/** Makes the in-process topology harness selectable as the Pub/Sub backend. */
public final class PubSubTopologyTestHarnessFactory implements TopologyTestHarness.Factory {
    @Override
    public String backendName() {
        return PubSubStreamBackend.PUBSUB.name();
    }

    @Override
    public TopologyTestHarness create(String appId, boolean tracing) {
        return new InMemoryTopologyTestHarness(appId, tracing);
    }
}
