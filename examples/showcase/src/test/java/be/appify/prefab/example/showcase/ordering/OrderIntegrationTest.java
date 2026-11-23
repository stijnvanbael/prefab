package be.appify.prefab.example.showcase.ordering;

import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Placing and cancelling orders. Publishing the events they raise is verified by the other tests of this package. */
@IntegrationTest
class OrderIntegrationTest {

    @Autowired
    OrderClient orders;
    @Autowired
    Shop shop;

    String customer;
    String product;

    @BeforeEach
    void createCustomerAndProduct() throws Exception {
        customer = shop.registerCustomer();
        product = shop.stockedProduct(100).id();
    }

    @Test
    void createOrder_givenQuantity_totalsUnitPriceTimesQuantity() throws Exception {
        var orderId = orders.createOrder(customer, product, 2, new BigDecimal("349.99"));

        var order = orders.getOrderById(orderId);

        assertThat(order.customer().id()).isEqualTo(customer);
        assertThat(order.product().id()).isEqualTo(product);
        assertThat(order.quantity()).isEqualTo(2);
        assertThat(order.total()).isEqualByComparingTo("699.98");
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void createOrder_givenZeroQuantity_isRejected() {
        assertThatThrownBy(() -> orders.createOrder(customer, product, 0, BigDecimal.TEN))
                .hasMessageContaining("was:<400>");
    }

    @Test
    void cancel_givenPlacedOrder_cancelsIt() throws Exception {
        var orderId = orders.createOrder(customer, product, 1, BigDecimal.TEN);

        orders.cancel(orderId);

        assertThat(orders.getOrderById(orderId).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void findOrders_givenCustomer_onlyListsOrdersOfThatCustomer() throws Exception {
        var other = shop.registerCustomer();
        var mine = orders.createOrder(customer, product, 1, BigDecimal.TEN);
        orders.createOrder(other, product, 1, BigDecimal.TEN);

        var found = orders.findOrders(Pageable.unpaged(), customer);

        assertThat(found.getContent()).extracting(order -> order.id().id()).containsExactly(mine);
    }
}
