package com.example.ordering;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import com.example.ordering.application.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class OrderRevenueAcceptanceTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    OrderRepository orders;

    @Test
    void sumsTotalsPerStatus() throws Exception {
        var api = new Api(mockMvc);
        var first = api.create("/orders", "{\"customerEmail\": \"ada@example.com\", \"total\": 10.50}");
        var second = api.create("/orders", "{\"customerEmail\": \"ada@example.com\", \"total\": 20}");
        api.create("/orders", "{\"customerEmail\": \"grace@example.com\", \"total\": 30}");
        api.postEmpty("/orders/{id}/ship", first);
        api.postEmpty("/orders/{id}/ship", second);

        assertThat(orders.totalRevenue("SHIPPED")).isEqualByComparingTo("30.50");
        assertThat(orders.totalRevenue("OPEN")).isEqualByComparingTo("30");
    }

    @Test
    void revenueIsZeroWithoutOrders() {
        assertThat(orders.totalRevenue("SHIPPED")).isEqualByComparingTo("0");
    }
}
