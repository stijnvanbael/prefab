package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.WordBatch;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class FilterMapTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void filter_shouldDropRecordsNotMatchingPredicate() {
        streams.from(IncomingOrder.class)
                .filter(order -> order.customer().id().startsWith("A"))
                .to("orders.filtered");

        var inputTopic = topology.input(IncomingOrder.class);
        var outputTopic = topology.output("orders.filtered");

        inputTopic.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")));
        inputTopic.send(new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("Bob")));
        inputTopic.send(new IncomingOrder(Reference.fromId("o-3"), Reference.fromId("Anna")));

        assertThat(outputTopic.values()).hasSize(2);
    }

    @Test
    void map_shouldTransformValues() {
        streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        var inputTopic = topology.input(IncomingOrder.class);
        var outputTopic = topology.output(ProcessedOrder.class);

        inputTopic.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")));

        var forwarded = outputTopic.value();
        assertThat(forwarded).isInstanceOf(ProcessedOrder.class);
        assertThat(forwarded).isEqualTo(new ProcessedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE")));
    }

    @Test
    void flatMap_shouldExpandOneRecordToMany() {
        streams.from(WordBatch.class)
                .flatMap(batch -> Stream.of(batch.words().split(","))
                        .map(word -> new WordBatch(batch.batchId(), word))
                        .toList())
                .to("words.out");

        var inputTopic = topology.input(WordBatch.class);
        var outputTopic = topology.output("words.out");

        inputTopic.send(new WordBatch(Reference.fromId("b-1"), "hello,world,foo"));

        assertThat(outputTopic.values()).hasSize(3);
    }
}
