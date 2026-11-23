package be.appify.prefab.example.showcase.ordering;

import be.appify.prefab.example.showcase.customer.CustomerClient;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * {@code @Multicast}: one {@code CustomerDeactivated} event is delivered to every order of that customer. A multicast
 * fails when its query finds nothing, so only customers with orders are deactivated here (TASK-357).
 */
@IntegrationTest
class OrderCancellationOnDeactivationIntegrationTest {

    @Autowired
    CustomerClient customers;
    @Autowired
    OrderClient orders;
    @Autowired
    Shop shop;

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
