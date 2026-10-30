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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static be.appify.prefab.streams.topology.TopologyFixtures.countOrdersPerCustomer;
import static be.appify.prefab.streams.topology.TopologyFixtures.customerKeyType;
import static org.assertj.core.api.Assertions.assertThat;

class MessageTracingAggregationTopologyTest {
    @RegisterExtension
    static final TopologyTestExtension EXTENSION = TopologyTestExtension.withTracing();

    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void aggregate_shouldCorrelateResultToAllFoldedMessages() {
        streams.from(IncomingOrder.class)
                .groupBy(customerKeyType(), IncomingOrder::customer)
                .aggregate(
                        OrderCount.class,
                        () -> new OrderCount(null, 0),
                        (order, count) -> new OrderCount(order.customer(), count.numberOfOrders() + 1))
                .to(OrderCount.class);
        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        input.sendWithMessageId(order("o-1", "alice"), "m-1");
        input.sendWithMessageId(order("o-2", "bob"), "m-2");
        input.sendWithMessageId(order("o-3", "alice"), "m-3");

        var messages = output.messages();
        assertThat(messages).extracting(message -> message.correlationIds())
                .containsExactly(List.of("m-1"), List.of("m-2"), List.of("m-1", "m-3"));
        assertThat(messages).allSatisfy(message -> assertThat(message.messageId()).isPresent());
    }

    @Test
    void windowedAggregate_shouldOnlyCorrelateToMessagesInTheWindow() {
        countOrdersPerCustomer(streams, AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO));
        var input = topology.input(IncomingOrder.class);
        var output = topology.output(OrderCount.class);

        input.sendWithMessageId(order("o-1", "alice"), "m-1", Instant.ofEpochMilli(1_000L));
        input.sendWithMessageId(order("o-2", "alice"), "m-2", Instant.ofEpochMilli(4_000L));
        input.sendWithMessageId(order("o-3", "alice"), "m-3", Instant.ofEpochMilli(11_000L));

        var messages = output.messages();
        assertThat(messages.getLast().correlationIds()).containsExactly("m-3");
        assertThat(messages.get(1).correlationIds()).containsExactly("m-1", "m-2");
    }

    private static IncomingOrder order(String id, String customer) {
        return new IncomingOrder(Reference.fromId(id), Reference.fromId(customer));
    }
}
