package com.example.shop.ordering;

import com.example.shop.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code @ViewOf}: an aggregate-bound view model with a derived field, served from the order's own list endpoint. */
@IntegrationTest
class OrderOverviewIntegrationTest {

    @Autowired
    OrderClient orders;
    @Autowired
    OrderOverviewClient overviews;
    @Autowired
    Shop shop;

    @Test
    void findOrderOverviews_givenOrders_derivesWhetherTheyCanBeCancelled() throws Exception {
        var customer = shop.registerCustomer();
        var product = shop.stockedProduct(10).id();
        var open = orders.createOrder(customer, product, 3, new BigDecimal("2.50"));
        var cancelled = orders.createOrder(customer, product, 1, new BigDecimal("2.50"));
        orders.cancel(cancelled);

        var found = overviews.findOrderOverviews(Pageable.unpaged(), customer).getContent();

        assertThat(found).anySatisfy(overview -> {
            assertThat(overview.order().id()).isEqualTo(open);
            assertThat(overview.total()).isEqualByComparingTo("7.50");
            assertThat(overview.cancellable()).isTrue();
        });
        assertThat(found).anySatisfy(overview -> {
            assertThat(overview.order().id()).isEqualTo(cancelled);
            assertThat(overview.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(overview.cancellable()).isFalse();
        });
    }
}
