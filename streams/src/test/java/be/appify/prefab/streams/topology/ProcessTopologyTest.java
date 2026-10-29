package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeIncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeOrderCount;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeOrderKey;
import be.appify.prefab.streams.topology.TopologyFixtures.CountCompositeOrderProcessor;
import be.appify.prefab.streams.topology.TopologyFixtures.CountOrderProcessor;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.OrderCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class ProcessTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void process_shouldPerformStatefulOperations() {
        streams.from(IncomingOrder.class)
                .process(new CountOrderProcessor())
                .to("prefab-streams-test-order-count");

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
    void end_shouldReturnDefinitionOfTopologyWithoutSink() {
        streams.from(IncomingOrder.class)
                .process(new CountOrderProcessor())
                .end();

        assertThat(topology.inspect().subtopologyCount()).isPositive();
    }

    @Test
    void process_shouldPerformStatefulOperationsForCompositeKeys() {
        streams.from(CompositeIncomingOrder.class)
                .process(new CountCompositeOrderProcessor())
                .to("prefab-streams-test-composite-count");

        var beO1 = new CompositeOrderKey("be", "o-1");
        var nlO1 = new CompositeOrderKey("nl", "o-1");
        var input = topology.input(CompositeIncomingOrder.class, CompositeOrderKey.class);
        var output = topology.output("prefab-streams-test-composite-count");

        input.send(new CompositeIncomingOrder(beO1, "alice"));
        input.send(new CompositeIncomingOrder(nlO1, "bob"));
        input.send(new CompositeIncomingOrder(beO1, "alice"));

        var mapper = JsonMapper.builder().findAndAddModules().build();
        var values = output.values().stream()
                .map(bytes -> {
                    try {
                        return mapper.readValue(bytes, CompositeOrderCount.class);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to deserialize composite order count", e);
                    }
                })
                .toList();

        assertThat(values)
                .containsExactlyInAnyOrder(
                        new CompositeOrderCount(beO1, 1),
                        new CompositeOrderCount(nlO1, 1),
                        new CompositeOrderCount(beO1, 2)
                );
    }
}
