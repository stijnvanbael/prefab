package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.example.showcase.ordering.OrderClient;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.inmemory.messaging.InMemoryEventBus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the order sales analytics topology of the showcase on the in-memory event bus: an order placed through the
 * REST API reaches the topology as an event, and the running revenue per category comes back on the bus.
 */
@IntegrationTest
class InMemoryOrderSalesAnalyticsIntegrationTest {
    @Autowired
    OrderClient orders;
    @Autowired
    Shop shop;
    @Autowired
    InMemoryEventBus bus;
    @Autowired
    EventRegistry eventRegistry;

    @Test
    void placeOrder_givenCatalogProduct_publishesRunningRevenueOfItsCategory() throws Exception {
        var customer = shop.registerCustomer();
        var product = shop.stockedProduct(100).id();
        bus.dispatch(new CatalogProduct(product, "books"));
        var revenue = revenueUpdates();

        orders.createOrder(customer, product, 2, new BigDecimal("10.00"));
        orders.createOrder(customer, product, 1, new BigDecimal("5.00"));

        assertThat(revenue).hasSize(3).last().satisfies(total -> {
            assertThat(total.categoryId()).isEqualTo("books");
            assertThat(total.itemCount()).isEqualTo(3);
            assertThat(total.revenue()).isEqualByComparingTo("25.00");
        });
    }

    @Test
    void placeOrder_givenUnknownProduct_countsTheSaleAsUncategorized() throws Exception {
        var customer = shop.registerCustomer();
        var product = shop.stockedProduct(100).id();
        var revenue = revenueUpdates();

        orders.createOrder(customer, product, 1, new BigDecimal("7.00"));

        assertThat(revenue).singleElement().satisfies(total -> {
            assertThat(total.categoryId()).isEqualTo("uncategorized");
            assertThat(total.revenue()).isEqualByComparingTo("7.00");
        });
    }

    private List<CategoryRevenue> revenueUpdates() {
        var updates = new ArrayList<CategoryRevenue>();
        bus.subscribe(eventRegistry.topicForType(CategoryRevenue.class), "test",
                message -> updates.add((CategoryRevenue) message.decode()));
        return updates;
    }
}
