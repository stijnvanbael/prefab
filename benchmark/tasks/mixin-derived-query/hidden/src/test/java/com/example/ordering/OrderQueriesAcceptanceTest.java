package com.example.ordering;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import com.example.ordering.application.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class OrderQueriesAcceptanceTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    OrderRepository orders;

    @Test
    void findsOrdersOfACustomerAndCountsByStatus() throws Exception {
        var api = new Api(mockMvc);
        var first = api.create("/orders", "{\"customerEmail\": \"ada@example.com\", \"total\": 10}");
        api.create("/orders", "{\"customerEmail\": \"ada@example.com\", \"total\": 20}");
        api.create("/orders", "{\"customerEmail\": \"grace@example.com\", \"total\": 30}");
        api.postEmpty("/orders/{id}/ship", first);

        assertThat(orders.findByCustomerEmail("ada@example.com"))
                .extracting(order -> order.total().intValue())
                .containsExactlyInAnyOrder(10, 20);
        assertThat(orders.findByCustomerEmail("nobody@example.com")).isEmpty();
        assertThat(orders.countByStatus(OrderStatus.OPEN)).isEqualTo(2);
        assertThat(orders.countByStatus(OrderStatus.SHIPPED)).isEqualTo(1);
    }
}
