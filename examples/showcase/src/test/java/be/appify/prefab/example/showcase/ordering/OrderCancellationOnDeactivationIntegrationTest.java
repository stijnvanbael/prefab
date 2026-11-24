package be.appify.prefab.example.showcase.ordering;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.customer.CustomerClient;
import be.appify.prefab.example.showcase.customer.CustomerDeactivated;
import be.appify.prefab.example.showcase.customer.application.CustomerOrderHistoryService;
import be.appify.prefab.example.showcase.ordering.application.OrderService;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * {@code @Multicast}: one {@code CustomerDeactivated} event is delivered to every order of that customer. Empty
 * queries are a no-op for order cancellation, while the default still fails when a target is expected (TASK-365).
 */
@IntegrationTest
class OrderCancellationOnDeactivationIntegrationTest {

    @Autowired
    CustomerClient customers;
    @Autowired
    OrderClient orders;
    @Autowired
    OrderService orderService;
    @Autowired
    CustomerOrderHistoryService customerOrderHistoryService;
    @Autowired
    Shop shop;

    @Test
    void deactivateCustomerWithoutOrders_doesNotFailWhenMulticastFindsNothing() {
        var event = new CustomerDeactivated(Reference.fromId(UUID.randomUUID().toString()));

        assertThatCode(() -> orderService.onCustomerDeactivated(event)).doesNotThrowAnyException();
    }

    @Test
    void multicastWithoutOptOut_failsWhenQueryFindsNothing() {
        var event = new OrderEvent.Placed(
                Reference.create(),
                Reference.fromId(UUID.randomUUID().toString()),
                Reference.create(),
                1,
                BigDecimal.TEN
        );

        assertThatThrownBy(() -> customerOrderHistoryService.onOrderPlaced(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No aggregates found for event");
    }

    @Test
    void deactivateCustomer_cancelsAllOrdersOfThatCustomer() throws Exception {
        var leaving = shop.registerCustomer();
        var staying = shop.registerCustomer();
        var product = shop.stockedProduct(10).id();
        var first = orders.createOrder(leaving, product, 1, BigDecimal.TEN);
        var second = orders.createOrder(leaving, product, 2, BigDecimal.ONE);
        var unrelated = orders.createOrder(staying, product, 1, BigDecimal.TEN);

        customers.deactivate(leaving);

        await().atMost(ofSeconds(20)).untilAsserted(() -> {
            assertThat(orders.getOrderById(first).status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(orders.getOrderById(second).status()).isEqualTo(OrderStatus.CANCELLED);
        });
        assertThat(customers.getCustomerById(leaving).active()).isFalse();
        assertThat(orders.getOrderById(unrelated).status()).isEqualTo(OrderStatus.PLACED);
    }
}
