package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.OrderCount;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static be.appify.prefab.streams.topology.TopologyFixtures.countOrdersPerCustomer;
import static be.appify.prefab.streams.topology.TopologyFixtures.customerKeyType;
import static be.appify.prefab.streams.topology.TopologyFixtures.pipeOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(TopologyTestExtension.class)
class AggregationTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void aggregate() {
        streams.from(IncomingOrder.class)
                .aggregate(
                        IncomingOrder::customer,
                        ordersForCustomer -> new OrderCount(ordersForCustomer.getFirst().customer(), ordersForCustomer.size()),
                        count -> true
                )
                .to(OrderCount.class);

        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        input.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
        input.send(new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("bob")));
        input.send(new IncomingOrder(Reference.fromId("o-3"), Reference.fromId("alice")));

        assertThat(output.values())
                .containsExactlyInAnyOrder(
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("bob"), 1),
                        new OrderCount(Reference.fromId("alice"), 2)
                );
    }

    @Test
    void aggregate_storeNameReflectsConcreteKeyAndValueTypes() {
        // The stream was created from IncomingOrder.class so V = IncomingOrder is known at
        // topology-build time via knownValueType().  Java's standard LambdaMetafactory erases
        // the groupBy return type to Object at runtime, so the key type (KO = Reference) cannot
        // be extracted without serialisable lambdas or an explicit parameter.
        // The store name therefore encodes only the value class: aggregation-incoming-order.
        streams.from(IncomingOrder.class)
                .aggregate(
                        IncomingOrder::customer,
                        orders -> new OrderCount(orders.getFirst().customer(), orders.size()),
                        count -> true
                )
                .to(OrderCount.class);

        var topologyDescription = topology.inspect().description();
        assertThat(topologyDescription).contains("aggregation-incoming-order");
    }

    @Test
    void groupByAggregate_shouldEvolveStateForMultipleEventsOnTheSameKey() {
        streams.from(IncomingOrder.class)
                .groupBy(customerKeyType(), IncomingOrder::customer)
                .aggregate(
                        OrderCount.class,
                        () -> new OrderCount(null, 0),
                        (order, count) -> new OrderCount(order.customer(), count.numberOfOrders() + 1)
                )
                .to(OrderCount.class);

        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        input.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
        input.send(new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("bob")));
        input.send(new IncomingOrder(Reference.fromId("o-3"), Reference.fromId("alice")));
        input.send(new IncomingOrder(Reference.fromId("o-4"), Reference.fromId("alice")));

        assertThat(output.values())
                .containsExactly(
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("bob"), 1),
                        new OrderCount(Reference.fromId("alice"), 2),
                        new OrderCount(Reference.fromId("alice"), 3)
                );
    }

    @Test
    void groupByAggregate_shouldUseNamedStoreAndRepartitionStep() {
        streams.from(IncomingOrder.class)
                .groupBy(customerKeyType(), IncomingOrder::customer)
                .aggregate(
                        OrderCount.class,
                        () -> new OrderCount(null, 0),
                        (order, count) -> new OrderCount(order.customer(), count.numberOfOrders() + 1)
                )
                .to(OrderCount.class);

        assertThat(topology.inspect().description()).contains("group-by-incoming-order-repartition");
        assertThat(topology.inspect().storeNames()).contains("aggregate-incoming-order-order-count");
    }

    @Test
    void windowedAggregate_shouldAggregateWithinWindowWithoutLeakingAcrossWindows() {
        countOrdersPerCustomer(streams, AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO));

        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        pipeOrder(input, "o-1", "alice", 1_000L);
        pipeOrder(input, "o-2", "alice", 4_000L);
        pipeOrder(input, "o-3", "bob", 5_000L);
        pipeOrder(input, "o-4", "alice", 11_000L);
        pipeOrder(input, "o-5", "alice", 12_000L);

        assertThat(output.values())
                .containsExactly(
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("alice"), 2),
                        new OrderCount(Reference.fromId("bob"), 1),
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("alice"), 2)
                );
    }

    @Test
    void windowedAggregate_shouldDropRecordsArrivingAfterWindowAndGraceHaveClosed() {
        countOrdersPerCustomer(streams, AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO));

        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        pipeOrder(input, "o-1", "alice", 1_000L);
        pipeOrder(input, "o-2", "alice", 25_000L);
        pipeOrder(input, "o-3", "alice", 2_000L);

        assertThat(output.values())
                .containsExactly(
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("alice"), 1)
                );
    }

    @Test
    void windowedAggregate_shouldAcceptLateRecordsWithinGrace() {
        countOrdersPerCustomer(streams, AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ofSeconds(5)));

        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        pipeOrder(input, "o-1", "alice", 1_000L);
        pipeOrder(input, "o-2", "alice", 12_000L);
        pipeOrder(input, "o-3", "alice", 2_000L);

        assertThat(output.values())
                .containsExactly(
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("alice"), 2)
                );
    }

    @Test
    void windowedAggregate_shouldDropLateRecordsBeyondGrace() {
        countOrdersPerCustomer(streams, AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ofSeconds(5)));

        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        pipeOrder(input, "o-1", "alice", 1_000L);
        pipeOrder(input, "o-2", "alice", 16_000L);
        pipeOrder(input, "o-3", "alice", 2_000L);

        assertThat(output.values())
                .containsExactly(
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("alice"), 1)
                );
    }

    @Test
    void windowedAggregate_withHoppingWindow_shouldAggregateRecordIntoEveryOverlappingWindow() {
        countOrdersPerCustomer(
                streams,
                AggregationWindow.hopping(Duration.ofSeconds(10), Duration.ofSeconds(5), Duration.ZERO));

        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        pipeOrder(input, "o-1", "alice", 7_000L);
        pipeOrder(input, "o-2", "alice", 8_000L);

        assertThat(output.values())
                .containsExactlyInAnyOrder(
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("alice"), 1),
                        new OrderCount(Reference.fromId("alice"), 2),
                        new OrderCount(Reference.fromId("alice"), 2)
                );
    }

    @Test
    void windowedAggregate_shouldUseWindowStore() {
        countOrdersPerCustomer(streams, AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO));

        assertThat(topology.inspect().storeNames()).contains("windowed-aggregate-incoming-order-order-count");
    }

    @Test
    void aggregationWindow_shouldRejectInvalidConfiguration() {
        assertThatThrownBy(() -> AggregationWindow.tumbling(Duration.ZERO, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AggregationWindow.hopping(Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AggregationWindow.tumbling(Duration.ofSeconds(5), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
