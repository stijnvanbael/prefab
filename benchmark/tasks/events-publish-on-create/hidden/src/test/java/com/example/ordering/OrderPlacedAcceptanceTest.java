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

@IntegrationTest
class OrderPlacedAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @TestEventConsumer(topic = "orders")
    EventConsumer<OrderPlaced> orders;

    @Test
    void creatingAnOrderPublishesOrderPlaced() throws Exception {
        var id = new Api(mockMvc).create("/orders", "{\"customer\": \"Ada\", \"total\": 42.50}");

        Api.eventually(Duration.ofSeconds(15), () -> assertThat(orders.messages())
                .anySatisfy(event -> {
                    assertThat(event.order().id()).isEqualTo(id);
                    assertThat(event.customer()).isEqualTo("Ada");
                    assertThat(event.total()).isEqualByComparingTo("42.50");
                }));
    }
}
