package be.appify.prefab.example.streams.sales;

import be.appify.prefab.example.streams.BiddingZoneId;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class SalesTracingTopologyTest {
    @RegisterExtension
    static final TopologyTestExtension EXTENSION = TopologyTestExtension.withTracing();

    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void shouldCorrelateRunningTotalToAllFoldedSales() {
        new SalesTotalsTopology().salesTotalsTopology(streams);
        var input = topology.input(EnergySale.class);
        var output = topology.output(ZoneSalesTotal.class);
        var belgium = new BiddingZoneId("BE");

        input.sendWithMessageId(new EnergySale("s-1", belgium, 10.0), "m-1");
        input.sendWithMessageId(new EnergySale("s-2", belgium, 2.5), "m-2");

        var messages = output.messages();
        assertThat(messages).hasSize(2);
        assertThat(messages.getLast().correlationIds()).containsExactly("m-1", "m-2");
        assertThat(messages.getLast().messageId()).isPresent();
    }
}
