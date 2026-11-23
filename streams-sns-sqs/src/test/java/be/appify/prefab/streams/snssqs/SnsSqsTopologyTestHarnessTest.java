package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.snssqs.StreamFixtures.CategoryTotal;
import be.appify.prefab.streams.snssqs.StreamFixtures.Sale;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.testing.TopologyTestHarness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Topology tests written against the backend-neutral test API, running on the SNS/SQS backend. */
class SnsSqsTopologyTestHarnessTest {
    @RegisterExtension
    static final TopologyTestExtension BACKEND = TopologyTestExtension.withBackend("sns-sqs");

    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void theHarnessIsSelectedByTheNameOfTheBackend() {
        var harness = TopologyTestHarness.create("sns-sqs", "shop", false);

        assertThat(harness).isInstanceOf(SnsSqsTopologyTestHarness.class);
    }

    @Test
    void aTopologyRunsThroughTheBackendNeutralApi() {
        streams.from(Sale.class)
                .groupBy(String.class, Sale::category)
                .aggregate(CategoryTotal.class, () -> new CategoryTotal(null, 0),
                        (sale, total) -> new CategoryTotal(sale.category(), total.total() + sale.amount()))
                .to(CategoryTotal.class);

        var sales = topology.input(Sale.class);
        var totals = topology.output(CategoryTotal.class);
        sales.send(new Sale("s-1", "books", 5));
        sales.send(new Sale("s-2", "books", 10));

        assertThat(totals.values()).containsExactly(new CategoryTotal("books", 5), new CategoryTotal("books", 15));
    }
}
