package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologyInspection;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.testing.TopologyTestHarness;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.JoinedOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ShippingUpdate;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTracingJoinTopologyTest {
    @RegisterExtension
    static final TopologyTestExtension EXTENSION = TopologyTestExtension.withTracing();

    private static final Pattern PROCESSOR = Pattern.compile("Processor: (\\S+)");

    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void join_shouldCorrelateResultToBothContributingMessages() {
        defineJoin(streams);

        topology.input(IncomingOrder.class).sendWithMessageId(order(), "m-order");
        topology.input(ShippingUpdate.class).sendWithMessageId(new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"), "m-ship");

        var message = topology.output(JoinedOrder.class).messages().getFirst();
        assertThat(message.correlationIds()).containsExactlyInAnyOrder("m-order", "m-ship");
        assertThat(message.messageId()).isPresent();
    }

    @Test
    void join_shouldKeepStoreAndInternalNamesIdenticalWithAndWithoutTracing() {
        var without = inspect(false);
        var with = inspect(true);

        assertThat(with.storeNames()).isEqualTo(without.storeNames());
        assertThat(applicationProcessors(without)).isNotEmpty();
        assertThat(applicationProcessors(with)).isEqualTo(applicationProcessors(without));
    }

    private static TopologyInspection inspect(boolean tracing) {
        var harness = TopologyTestHarness.create("name-stability", tracing);
        defineJoin(harness.streams());
        try (var session = harness.start()) {
            return session.inspect();
        }
    }

    private static List<String> applicationProcessors(TopologyInspection inspection) {
        return PROCESSOR.matcher(inspection.description()).results()
                .map(match -> match.group(1))
                .filter(name -> !name.contains("trace-"))
                .toList();
    }

    private static void defineJoin(PrefabStreams streams) {
        streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status()))
                .to(JoinedOrder.class);
    }

    private static IncomingOrder order() {
        return new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice"));
    }
}
