package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.OrderEvent;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSalesTracingTopologyTest {
    @RegisterExtension
    static final TopologyTestExtension EXTENSION = TopologyTestExtension.withTracing();

    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void shouldCorrelateRevenueToItsSourceOrder() {
        new OrderSalesAnalyticsConfiguration().orderSalesAnalytics(streams);
        topology.input(CatalogProduct.class).send(new CatalogProduct("product-1", "books"));
        topology.input(OrderEvent.class).sendWithMessageId(
                new OrderEvent.Placed(
                        Reference.fromId("order-1"),
                        Reference.fromId("customer-1"),
                        Reference.fromId("product-1"),
                        2,
                        new BigDecimal("20.00")
                ),
                "order-message"
        );

        var messages = topology.output("test-category-revenue", CategoryRevenue.class).messages();
        assertThat(messages).hasSize(2);
        assertThat(messages.getLast().correlationIds()).containsExactly("order-message");
        assertThat(messages.getLast().messageId()).isPresent();
    }
}
