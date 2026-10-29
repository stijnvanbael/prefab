package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.OrderEvent;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ShippingUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class BranchMergeTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void multipleStreams_shouldBeJoinedInOneTopology() {
        streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        streams.from(ProcessedOrder.class)
                .map(order -> new ShippingUpdate(order.orderId(), "PENDING"))
                .to(ShippingUpdate.class);

        var input = topology.input(IncomingOrder.class);
        var output = topology.output(ShippingUpdate.class);

        input.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

        assertThat(output.values())
                .containsExactlyInAnyOrder(
                        new ShippingUpdate(Reference.fromId("o-1"), "PENDING")
                );
    }

    @Test
    void mergeAfterMap_shouldUseTheRightSerdes() {
        var orders = streams.from(IncomingOrder.class)
                .map(order -> new OrderEvent.OrderCreated(order.orderId(), order.customer()));
        var shippingUpdates = streams.from(ShippingUpdate.class)
                .filter(update -> "SHIPPED".equals(update.status()))
                .map(update -> new OrderEvent.OrderShipped(update.orderId()));
        streams.merge(orders, shippingUpdates)
                .to(OrderEvent.class);

        var inputOrder = topology.input(IncomingOrder.class);
        var inputShippingUpdate = topology.input(ShippingUpdate.class);
        var output = topology.output(OrderEvent.class);

        inputOrder.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
        inputShippingUpdate.send(new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"));

        assertThat(output.values())
                .containsExactlyInAnyOrder(
                        new OrderEvent.OrderCreated(Reference.fromId("o-1"), Reference.fromId("alice")),
                        new OrderEvent.OrderShipped(Reference.fromId("o-1"))
                );
    }
}
