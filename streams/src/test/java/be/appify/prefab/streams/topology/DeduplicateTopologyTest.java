package be.appify.prefab.streams.topology;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static be.appify.prefab.streams.topology.TopologyFixtures.pipeOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(TopologyTestExtension.class)
class DeduplicateTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void deduplicate_shouldSuppressRepeatedEventIdentitiesAndEmitUniqueEvents() {
        streams.from(IncomingOrder.class)
                .deduplicate(order -> order.orderId().id(), Duration.ofMinutes(1))
                .to("orders.deduplicated");

        var input = topology.input(IncomingOrder.class);
        var output = topology.output("orders.deduplicated");

        pipeOrder(input, "o-1", "alice", 1_000L);
        pipeOrder(input, "o-1", "alice", 2_000L);
        pipeOrder(input, "o-2", "bob", 3_000L);
        pipeOrder(input, "o-1", "alice", 4_000L);

        assertThat(output.values()).hasSize(2);
    }

    @Test
    void deduplicate_shouldEmitAgainOnceRetentionHasPassed() {
        streams.from(IncomingOrder.class)
                .deduplicate(order -> order.orderId().id(), Duration.ofSeconds(10))
                .to("orders.deduplicated");

        var input = topology.input(IncomingOrder.class);
        var output = topology.output("orders.deduplicated");

        pipeOrder(input, "o-1", "alice", 1_000L);
        pipeOrder(input, "o-1", "alice", 5_000L);
        pipeOrder(input, "o-1", "alice", 60_000L);

        assertThat(output.values()).hasSize(2);
    }

    @Test
    void deduplicate_withoutExtractor_shouldUseRecordKeyAsIdentity() {
        streams.from(IncomingOrder.class)
                .deduplicate(Duration.ofMinutes(1))
                .to("orders.deduplicated");

        var input = topology.input(IncomingOrder.class);
        var output = topology.output("orders.deduplicated");

        pipeOrder(input, "o-1", "alice", 1_000L);
        pipeOrder(input, "o-1", "bob", 2_000L);
        pipeOrder(input, "o-2", "alice", 3_000L);

        assertThat(output.values()).hasSize(2);
    }

    @Test
    void deduplicate_shouldUseWindowStore() {
        streams.from(IncomingOrder.class)
                .deduplicate(order -> order.orderId().id(), Duration.ofMinutes(1))
                .to("orders.deduplicated");

        assertThat(topology.inspect().storeNames()).contains("deduplicate-incoming-order");
    }

    @Test
    void deduplicate_shouldRejectNonPositiveRetention() {
        var stream = streams.from(IncomingOrder.class);

        assertThatThrownBy(() -> stream.deduplicate(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
