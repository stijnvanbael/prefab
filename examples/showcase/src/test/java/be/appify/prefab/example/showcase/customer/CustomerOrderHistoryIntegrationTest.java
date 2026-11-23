package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.example.showcase.capability.RequiresCapability;
import be.appify.prefab.example.showcase.customer.infrastructure.http.CustomerOrderHistoryResponse;
import be.appify.prefab.example.showcase.ordering.OrderClient;
import be.appify.prefab.example.showcase.ordering.OrderStatus;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static be.appify.prefab.example.showcase.capability.Capability.ORDERED_DELIVERY;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/** A projection (read model) that is created from {@link CustomerRegistered} and kept up to date by order events. */
@IntegrationTest
class CustomerOrderHistoryIntegrationTest {

    @Autowired
    CustomerOrderHistoryClient histories;
    @Autowired
    OrderClient orders;
    @Autowired
    Shop shop;

    String customer;
    String product;

    @BeforeEach
    void registerCustomer() throws Exception {
        customer = shop.registerCustomer();
        product = shop.stockedProduct(100).id();
    }

    @Test
    void registerCustomer_startsAnEmptyHistory() {
        assertThat(historyOf(customer).orders()).isEmpty();
    }

    @Test
    void placeOrder_addsItToTheHistoryOfTheCustomerOnly() throws Exception {
        var other = shop.registerCustomer();

        var orderId = orders.createOrder(customer, product, 1, BigDecimal.TEN);

        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(historyOf(customer).orders())
                        .extracting(entry -> entry.order().id(), CustomerOrderHistory.Entry::status)
                        .containsExactly(tuple(orderId, OrderStatus.PLACED)));
        assertThat(historyOf(other).orders()).isEmpty();
    }

    /**
     * The placed and cancelled event of one order are consumed by the same consumer, in the order they were
     * published, because they share a partitioning key. If the cancellation overtook the placement, the history
     * would never learn about it.
     */
    @Test
    @RequiresCapability(ORDERED_DELIVERY)
    void cancelOrderRightAfterPlacing_endsUpCancelledInTheHistory() throws Exception {
        for (var i = 0; i < 5; i++) {
            var orderId = orders.createOrder(customer, product, 1, BigDecimal.TEN);
            orders.cancel(orderId);
        }

        await().atMost(ofSeconds(30)).untilAsserted(() ->
                assertThat(historyOf(customer).orders())
                        .hasSize(5)
                        .allSatisfy(entry -> assertThat(entry.status()).isEqualTo(OrderStatus.CANCELLED)));
    }

    private CustomerOrderHistoryResponse historyOf(String customerId) {
        return await().atMost(ofSeconds(20)).until(
                () -> histories.findCustomerOrderHistories(Pageable.unpaged(), customerId).getContent().stream().findFirst(),
                found -> found.isPresent()).orElseThrow();
    }
}
