package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeIncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.CompositeOrderKey;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class KeyFormatTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void to_shouldEmitPlainUtf8KeyBytesForSingleFieldRecordKeys() {
        streams.from(IncomingOrder.class)
                .to("orders.single-field-key");

        var input = topology.input(IncomingOrder.class);
        var output = topology.output("orders.single-field-key");

        input.send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

        var record = output.records().getFirst();
        assertThat(record.key()).isEqualTo("o-1");
    }

    @Test
    void to_shouldEmitJsonKeyBytesForCompositeRecordKeys() {
        streams.from(CompositeIncomingOrder.class)
                .to("orders.composite-key");

        var key = new CompositeOrderKey("be", "o-1");
        var input = topology.input(CompositeIncomingOrder.class, CompositeOrderKey.class);
        var output = topology.output("orders.composite-key");

        input.send(new CompositeIncomingOrder(key, "alice"));

        var record = output.records().getFirst();
        assertThat(record.key())
                .contains("\"biddingZone\":\"be\"")
                .contains("\"orderId\":\"o-1\"");
    }
}
