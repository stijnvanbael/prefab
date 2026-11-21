package com.example.ordering;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@IntegrationTest
class OrderPaymentAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void paymentsAccumulateUntilTheOrderIsPaid() throws Exception {
        var api = new Api(mockMvc);
        var order = api.create("/orders", "{\"customer\": \"Ada\", \"total\": 100}");

        api.create("/payments", "{\"order\": \"%s\", \"amount\": 40}".formatted(order));
        Api.eventually(Duration.ofSeconds(20), () -> {
            var response = api.get("/orders/{id}", order).andExpect(jsonPath("$.status").value("OPEN"));
            assertThat(Api.number(response, "$.amountPaid")).isCloseTo(40.0, within(0.001));
        });

        api.create("/payments", "{\"order\": \"%s\", \"amount\": 60}".formatted(order));
        Api.eventually(Duration.ofSeconds(20), () -> {
            var response = api.get("/orders/{id}", order).andExpect(jsonPath("$.status").value("PAID"));
            assertThat(Api.number(response, "$.amountPaid")).isCloseTo(100.0, within(0.001));
        });
    }

    @Test
    void paymentsForOtherOrdersDoNotCount() throws Exception {
        var api = new Api(mockMvc);
        var order = api.create("/orders", "{\"customer\": \"Ada\", \"total\": 50}");
        var other = api.create("/orders", "{\"customer\": \"Grace\", \"total\": 50}");

        api.create("/payments", "{\"order\": \"%s\", \"amount\": 50}".formatted(other));
        Api.eventually(Duration.ofSeconds(20), () ->
                api.get("/orders/{id}", other).andExpect(jsonPath("$.status").value("PAID")));

        var response = api.get("/orders/{id}", order).andExpect(jsonPath("$.status").value("OPEN"));
        assertThat(Api.number(response, "$.amountPaid")).isCloseTo(0.0, within(0.001));
    }
}
