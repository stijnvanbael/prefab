package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTracingTopologyTest {
    @RegisterExtension
    static final TopologyTestExtension EXTENSION = TopologyTestExtension.withTracing();

    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void to_shouldAssignMessageIdToPublishedMessage() {
        streams.from(IncomingOrder.class).map(MessageTracingTopologyTest::processed).to(ProcessedOrder.class);

        topology.input(IncomingOrder.class).send(order("o-1"));

        var message = topology.output(ProcessedOrder.class).messages().getFirst();
        assertThat(message.messageId()).isPresent();
    }

    @Test
    void from_shouldAssignRandomIdToConsumedMessageWithoutId() {
        streams.from(IncomingOrder.class).map(MessageTracingTopologyTest::processed).to(ProcessedOrder.class);

        topology.input(IncomingOrder.class).send(order("o-1"));

        var message = topology.output(ProcessedOrder.class).messages().getFirst();
        assertThat(message.correlationIds()).hasSize(1).doesNotContain(message.messageId().orElseThrow());
    }

    @Test
    void from_shouldUseExistingMessageIdAsCorrelation() {
        streams.from(IncomingOrder.class).map(MessageTracingTopologyTest::processed).to(ProcessedOrder.class);

        topology.input(IncomingOrder.class).sendWithMessageId(order("o-1"), "m-1");

        var message = topology.output(ProcessedOrder.class).messages().getFirst();
        assertThat(message.correlationIds()).containsExactly("m-1");
        assertThat(message.messageId()).isPresent().get().isNotEqualTo("m-1");
    }

    @Test
    void map_inChain_shouldKeepCorrelationOfConsumedMessage() {
        streams.from(IncomingOrder.class)
                .map(o -> new ProcessedOrder(o.orderId(), o.customer()))
                .map(o -> new ProcessedOrder(o.orderId(), Reference.fromId("changed")))
                .filter(o -> true)
                .to(ProcessedOrder.class);

        topology.input(IncomingOrder.class).sendWithMessageId(order("o-1"), "m-1");

        var message = topology.output(ProcessedOrder.class).messages().getFirst();
        assertThat(message.correlationIds()).containsExactly("m-1");
    }

    @Test
    void flatMap_shouldCorrelateEveryOutputToConsumedMessageWithOwnId() {
        streams.from(IncomingOrder.class)
                .flatMap(order -> List.of(processed(order), processed(order)))
                .to(ProcessedOrder.class);
        var output = topology.output(ProcessedOrder.class);

        topology.input(IncomingOrder.class).sendWithMessageId(order("o-1"), "m-1");

        var messages = output.messages();
        assertThat(messages).hasSize(2);
        assertThat(messages).allSatisfy(message -> assertThat(message.correlationIds()).containsExactly("m-1"));
        assertThat(messages.stream().map(message -> message.messageId().orElseThrow()).distinct()).hasSize(2);
    }

    @Test
    void repartition_shouldKeepCorrelation() {
        streams.from(IncomingOrder.class)
                .repartition()
                .map(o -> new ProcessedOrder(o.orderId(), o.customer()))
                .to(ProcessedOrder.class);

        topology.input(IncomingOrder.class).sendWithMessageId(order("o-1"), "m-1");

        assertThat(topology.output(ProcessedOrder.class).messages().getFirst().correlationIds())
                .isEqualTo(List.of("m-1"));
    }

    private static ProcessedOrder processed(IncomingOrder order) {
        return new ProcessedOrder(order.orderId(), order.customer());
    }

    private static IncomingOrder order(String id) {
        return new IncomingOrder(Reference.fromId(id), Reference.fromId("Alice"));
    }
}
