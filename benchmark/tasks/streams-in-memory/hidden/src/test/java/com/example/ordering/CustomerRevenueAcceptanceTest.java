package com.example.ordering;

import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.IntegrationTest;
import be.appify.prefab.test.TestEventConsumer;
import benchmark.support.Api;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
class CustomerRevenueAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @TestEventConsumer(topic = "customer-revenue")
    EventConsumer<CustomerRevenue> revenue;

    @Test
    void everyOrderPublishesTheRunningTotalOfItsCustomer() throws Exception {
        var api = new Api(mockMvc);

        api.create("/orders", "{\"customer\": \"Ada\", \"total\": 10.00}");
        api.create("/orders", "{\"customer\": \"Grace\", \"total\": 7.00}");
        api.create("/orders", "{\"customer\": \"Ada\", \"total\": 5.50}");

        Api.eventually(Duration.ofSeconds(15), () -> assertThat(revenue.messages())
                .extracting(CustomerRevenue::customer, total -> total.total().setScale(2).toPlainString())
                .containsExactly(
                        tuple("Ada", "10.00"),
                        tuple("Grace", "7.00"),
                        tuple("Ada", "15.50")));
    }
}
