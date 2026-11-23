package be.appify.prefab.streams.snssqs;

import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Starts the declared topology once all singletons exist, and stops it when the context closes.
 *
 * <p>The engine registers its routes as soon as {@link StreamDefinition} beans end a stream, so asking for all of
 * them first guarantees that every topology bean has run before the subscriptions start.
 */
final class SnsSqsTopologyLifecycle implements SmartInitializingSingleton, DisposableBean {
    private final InProcessPrefabStreams streams;
    private final ObjectProvider<StreamDefinition> streamDefinitions;
    private InProcessPrefabStreams.RunningTopology topology;

    SnsSqsTopologyLifecycle(InProcessPrefabStreams streams, ObjectProvider<StreamDefinition> streamDefinitions) {
        this.streams = streams;
        this.streamDefinitions = streamDefinitions;
    }

    @Override
    public void afterSingletonsInstantiated() {
        streamDefinitions.orderedStream().forEach(definition -> { });
        topology = streams.start();
    }

    @Override
    public void destroy() {
        if (topology != null) {
            topology.close();
        }
    }
}
