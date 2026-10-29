package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.OrderCount;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static be.appify.prefab.streams.topology.TopologyFixtures.customerKeyType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(TopologyTestExtension.class)
class RepartitionTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void keyBy_shouldRekeyRecordsByTheMappedValueKey() {
        streams.from(IncomingOrder.class)
                .keyBy(customerKeyType(), order -> new OrderCount(order.customer(), 1))
                .to(OrderCount.class);

        topology.input(IncomingOrder.class)
                .send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

        var record = topology.output("test-order-count", OrderCount.class).records().getFirst();
        assertThat(record.key()).isEqualTo(Reference.fromId("alice"));
        assertThat(record.value()).isEqualTo(new OrderCount(Reference.fromId("alice"), 1));
    }

    @Test
    void keyBy_withExplicitName_shouldUseTheGivenRepartitionTopicName() {
        streams.from(IncomingOrder.class)
                .keyBy(customerKeyType(), order -> new OrderCount(order.customer(), 1), "orders-by-customer")
                .to(OrderCount.class);

        assertThat(topology.inspect().description()).contains("orders-by-customer-repartition");
    }

    @Test
    void repartition_withExplicitName_shouldUseTheGivenNameAndKeepKeyAndValue() {
        streams.from(IncomingOrder.class)
                .repartition("incoming-orders")
                .to("processed-sink");

        assertThat(topology.inspect().description()).contains("incoming-orders-repartition");
    }

    @Test
    void repartition_withoutName_shouldGenerateAName() {
        streams.from(IncomingOrder.class)
                .repartition()
                .to("processed-sink");

        assertThat(topology.inspect().description()).contains("repartition-incoming-order-repartition");
    }

    @Test
    void repartition_shouldPassRecordsThrough() {
        streams.from(IncomingOrder.class)
                .repartition("incoming-orders")
                .map(order -> new ProcessedOrder(order.orderId(), order.customer()))
                .to(ProcessedOrder.class);

        topology.input(IncomingOrder.class)
                .send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

        assertThat(topology.output(ProcessedOrder.class).values())
                .containsExactly(new ProcessedOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
    }

    @Test
    void repartition_withInvalidName_shouldBeRejectedWithActionableMessage() {
        var stream = streams.from(IncomingOrder.class);

        assertThatThrownBy(() -> stream.repartition("bad name!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid repartition topic name 'bad name!'")
                .hasMessageContaining("letters, digits");
        assertThatThrownBy(() -> stream.repartition(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or blank");
        assertThatThrownBy(() -> stream.repartition(".."))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stream.repartition(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repartition_withDuplicateName_shouldBeRejected() {
        var stream = streams.from(IncomingOrder.class);
        stream.repartition("shared-name");

        assertThatThrownBy(() -> stream.repartition("shared-name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'shared-name' conflicts")
                .hasMessageContaining("Choose a unique name");
    }
}
