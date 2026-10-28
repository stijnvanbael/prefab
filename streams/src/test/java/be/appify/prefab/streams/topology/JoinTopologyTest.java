package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeIncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeJoinedOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeOrderKey;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeShippingUpdate;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.JoinedOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ShippingUpdate;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static be.appify.prefab.streams.topology.TopologyFixtures.joinOrdersWithShipping;
import static be.appify.prefab.streams.topology.TopologyFixtures.orderKey;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class JoinTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void join_shouldEmitResultWhenKeysMatchWithinWindow() {
        streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        var ordersInputTopic = topology.input(IncomingOrder.class);
        var shipmentsInputTopic = topology.input(ShippingUpdate.class);
        var outputTopic = topology.output(JoinedOrder.class);

        ordersInputTopic.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")), Instant.ofEpochMilli(1_000L));
        shipmentsInputTopic.send(new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"), Instant.ofEpochMilli(5_000L));

        assertThat(outputTopic.value())
                .isEqualTo(new JoinedOrder(Reference.fromId("o-1"), Reference.fromId("Alice"), "SHIPPED"));
    }

    @Test
    void join_shouldNotEmitResultWhenKeysDoNotMatch() {
        streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        var ordersInputTopic = topology.input(IncomingOrder.class);
        var shipmentsInputTopic = topology.input(ShippingUpdate.class);
        var outputTopic = topology.output(JoinedOrder.class);

        ordersInputTopic.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")), Instant.ofEpochMilli(1_000L));
        shipmentsInputTopic.send(new ShippingUpdate(Reference.fromId("o-2"), "SHIPPED"), Instant.ofEpochMilli(5_000L));

        assertThat(outputTopic.values()).isEmpty();
    }

    @Test
    void join_shouldNotEmitResultWhenRecordsAreOutsideWindow() {
        streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(2), Duration.ZERO),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        var ordersInputTopic = topology.input(IncomingOrder.class);
        var shipmentsInputTopic = topology.input(ShippingUpdate.class);
        var outputTopic = topology.output(JoinedOrder.class);

        ordersInputTopic.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")), Instant.ofEpochMilli(1_000L));
        shipmentsInputTopic.send(new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"), Instant.ofEpochMilli(5_000L));

        assertThat(outputTopic.values()).isEmpty();
    }

    @Test
    void join_shouldEmitResultWhenCompositeKeysMatchWithinWindow() {
        streams.from(CompositeIncomingOrder.class)
                .join(
                        streams.from(CompositeShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new CompositeJoinedOrder(order.key(), order.customer(), shipping.status())
                )
                .to(CompositeJoinedOrder.class);

        var key = new CompositeOrderKey("be", "o-1");
        var orders = topology.input(CompositeIncomingOrder.class, CompositeOrderKey.class);
        var updates = topology.input(CompositeShippingUpdate.class, CompositeOrderKey.class);
        var output = topology.output(CompositeJoinedOrder.class, CompositeOrderKey.class);

        orders.send(new CompositeIncomingOrder(key, "alice"), Instant.ofEpochMilli(1_000L));
        updates.send(new CompositeShippingUpdate(key, "SHIPPED"), Instant.ofEpochMilli(4_000L));

        assertThat(output.value())
                .isEqualTo(new CompositeJoinedOrder(key, "alice", "SHIPPED"));
    }

    @Test
    void join_shouldJoinLateRecordWithinGrace() {
        joinOrdersWithShipping(streams, Duration.ofSeconds(10));

        var orders = topology.input(IncomingOrder.class);
        var shipments = topology.input(ShippingUpdate.class);
        var output = topology.output(JoinedOrder.class);

        orders.send(new IncomingOrder(orderKey("o-2"), Reference.fromId("Bob")), Instant.ofEpochMilli(40_000L));
        orders.send(new IncomingOrder(orderKey("o-1"), Reference.fromId("Alice")), Instant.ofEpochMilli(15_000L));
        shipments.send(new ShippingUpdate(orderKey("o-1"), "SHIPPED"), Instant.ofEpochMilli(24_000L));

        assertThat(output.values())
                .containsExactly(new JoinedOrder(orderKey("o-1"), Reference.fromId("Alice"), "SHIPPED"));
    }

    @Test
    void join_shouldDropLateRecordBeyondGrace() {
        joinOrdersWithShipping(streams, Duration.ofSeconds(1));

        var orders = topology.input(IncomingOrder.class);
        var shipments = topology.input(ShippingUpdate.class);
        var output = topology.output(JoinedOrder.class);

        orders.send(new IncomingOrder(orderKey("o-2"), Reference.fromId("Bob")), Instant.ofEpochMilli(40_000L));
        orders.send(new IncomingOrder(orderKey("o-1"), Reference.fromId("Alice")), Instant.ofEpochMilli(15_000L));
        shipments.send(new ShippingUpdate(orderKey("o-1"), "SHIPPED"), Instant.ofEpochMilli(24_000L));

        assertThat(output.values()).isEmpty();
    }

    @Test
    void joinAfterMap_shouldUseTheRightSerdes() {
        streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(2), Duration.ZERO),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        var inputOrder = topology.input(IncomingOrder.class);
        var inputShippingUpdate = topology.input(ShippingUpdate.class);
        var output = topology.output(JoinedOrder.class);

        inputOrder.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
        inputShippingUpdate.send(new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"));

        assertThat(output.values())
                .containsExactlyInAnyOrder(
                        new JoinedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE"), "SHIPPED")
                );
    }
}
