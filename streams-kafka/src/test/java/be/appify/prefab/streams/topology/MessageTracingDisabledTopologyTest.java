package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class MessageTracingDisabledTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void tracing_whenDisabled_shouldNotAddHeaders() {
        streams.from(IncomingOrder.class)
                .map(o -> new ProcessedOrder(o.orderId(), o.customer()))
                .to(ProcessedOrder.class);

        topology.input(IncomingOrder.class)
                .sendWithMessageId(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")), "m-1");

        var message = topology.output(ProcessedOrder.class).messages().getFirst();
        assertThat(message.correlationIds()).isEmpty();
        assertThat(topology.inspect().description()).doesNotContain("trace-");
    }
}
