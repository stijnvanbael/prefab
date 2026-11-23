package be.appify.prefab.example.showcase.streams.kafka;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.capability.Capability;
import be.appify.prefab.example.showcase.capability.RequiresCapability;
import be.appify.prefab.example.showcase.ordering.Order;
import be.appify.prefab.example.showcase.streams.OrderRoute;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
@RequiresCapability(Capability.KAFKA_STREAMS_BREAKOUT)
class KafkaOrderTypeBreakoutTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void shouldUseNativeKafkaBreakoutToRekeyByOrderType() {
        new KafkaOrderTypeBreakoutConfiguration().kafkaOrderTypeBreakout(streams);
        Reference<Order> order = Reference.fromId("order-1");

        topology.input(OrderRoute.class).send(new OrderRoute(order, "placed"));

        assertThat(topology.output(KafkaOrderTypeRoute.class).values())
                .containsExactly(new KafkaOrderTypeRoute("placed", order));
    }
}
