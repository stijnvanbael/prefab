package be.appify.prefab.streams.topology;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static be.appify.prefab.streams.topology.TopologyFixtures.orderKey;
import static org.assertj.core.api.Assertions.assertThat;

class TopologyTestExtensionTest {
    @RegisterExtension
    static final TopologyTestExtension EXTENSION = TopologyTestExtension.withAppId("custom");

    @Test
    void extension_shouldInjectStreamsAndSessionAsParametersAndUseTheGivenAppId(
            @TestStreams PrefabStreams streams,
            @TestStreams TopologySession topology
    ) {
        streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), order.customer()))
                .to(ProcessedOrder.class);

        topology.input(IncomingOrder.class).send(new IncomingOrder(orderKey("o-1"), null));

        assertThat(topology.output("custom-processed-order").isEmpty()).isFalse();
    }
}
