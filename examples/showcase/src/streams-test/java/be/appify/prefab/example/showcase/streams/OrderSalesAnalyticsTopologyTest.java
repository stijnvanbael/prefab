package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.catalog.Product;
import be.appify.prefab.example.showcase.customer.Customer;
import be.appify.prefab.example.showcase.ordering.Order;
import be.appify.prefab.example.showcase.ordering.OrderEvent;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class OrderSalesAnalyticsTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void shouldAggregateRevenueAndItemsPerCategory() {
        new OrderSalesAnalyticsConfiguration().orderSalesAnalytics(streams);
        addCatalogProduct();

        var orders = topology.input(OrderEvent.class);
        var output = topology.output(CategoryRevenue.class);
        orders.send(placed("order-1", 2, "20.00"));
        orders.send(placed("order-2", 1, "5.00"));

        assertThat(output.values()).containsExactly(
                new CategoryRevenue("books", new BigDecimal("10.00"), 1),
                new CategoryRevenue("books", new BigDecimal("20.00"), 2),
                new CategoryRevenue("books", new BigDecimal("25.00"), 3)
        );
    }

    @Test
    void shouldCountSalesOfProductsOutsideTheCatalogAsUncategorized() {
        new OrderSalesAnalyticsConfiguration().orderSalesAnalytics(streams);

        var output = topology.output(CategoryRevenue.class);
        topology.input(OrderEvent.class).send(placed("order-1", 1, "10.00"));

        assertThat(output.values()).containsExactly(
                new CategoryRevenue("uncategorized", new BigDecimal("10.00"), 1)
        );
    }

    @Test
    void shouldAggregateHourlyRevenueAndAcceptLateOrdersWithinGrace() {
        new OrderSalesAnalyticsConfiguration().orderSalesAnalytics(streams);
        addCatalogProduct();

        var orders = topology.input(OrderEvent.class);
        var output = topology.output(HourlyCategoryRevenue.class);
        orders.send(placed("order-1", 1, "10.00"), Instant.parse("2026-01-01T10:05:00Z"));
        orders.send(placed("order-2", 1, "4.00"), Instant.parse("2026-01-01T11:02:00Z"));
        orders.send(placed("order-3", 1, "2.00"), Instant.parse("2026-01-01T10:59:00Z"));

        assertThat(output.values()).containsExactly(
                new HourlyCategoryRevenue("books", new BigDecimal("10.00"), 1),
                new HourlyCategoryRevenue("books", new BigDecimal("4.00"), 1),
                new HourlyCategoryRevenue("books", new BigDecimal("12.00"), 2)
        );
    }

    @Test
    void shouldDropOrdersBeyondTheHourlyGracePeriod() {
        new OrderSalesAnalyticsConfiguration().orderSalesAnalytics(streams);
        addCatalogProduct();

        var orders = topology.input(OrderEvent.class);
        var output = topology.output(HourlyCategoryRevenue.class);
        orders.send(placed("order-1", 1, "10.00"), Instant.parse("2026-01-01T10:05:00Z"));
        orders.send(placed("order-2", 1, "4.00"), Instant.parse("2026-01-01T11:10:00Z"));
        orders.send(placed("order-3", 1, "2.00"), Instant.parse("2026-01-01T10:59:00Z"));

        assertThat(output.values()).containsExactly(
                new HourlyCategoryRevenue("books", new BigDecimal("10.00"), 1),
                new HourlyCategoryRevenue("books", new BigDecimal("4.00"), 1)
        );
    }

    private void addCatalogProduct() {
        topology.input(CatalogProduct.class).send(new CatalogProduct("product-1", "books"));
    }

    private static OrderEvent.Placed placed(String id, int quantity, String total) {
        return new OrderEvent.Placed(
                Reference.<Order>fromId(id),
                Reference.<Customer>fromId("customer-1"),
                Reference.<Product>fromId("product-1"),
                quantity,
                new BigDecimal(total)
        );
    }
}
