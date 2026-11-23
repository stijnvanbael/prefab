package com.example.watch;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class PriceWatchAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void allWatchesOfAProductFollowItsPrice() throws Exception {
        var api = new Api(mockMvc);
        var lamp = api.create("/products", "{\"name\": \"Lamp\", \"price\": 20}");
        var desk = api.create("/products", "{\"name\": \"Desk\", \"price\": 200}");
        var adaWatch = api.create("/price-watches",
                "{\"email\": \"ada@example.com\", \"product\": \"%s\", \"currentPrice\": 20}".formatted(lamp));
        var graceWatch = api.create("/price-watches",
                "{\"email\": \"grace@example.com\", \"product\": \"%s\", \"currentPrice\": 20}".formatted(lamp));
        var deskWatch = api.create("/price-watches",
                "{\"email\": \"ada@example.com\", \"product\": \"%s\", \"currentPrice\": 200}".formatted(desk));

        api.put("/products/{id}/price", "{\"price\": 15}", lamp).andExpect(status().isOk());

        Api.eventually(Duration.ofSeconds(20), () -> {
            assertThat(Api.number(api.get("/price-watches/{id}", adaWatch), "$.currentPrice")).isCloseTo(15.0, within(0.001));
            assertThat(Api.number(api.get("/price-watches/{id}", graceWatch), "$.currentPrice")).isCloseTo(15.0, within(0.001));
        });
        assertThat(Api.number(api.get("/price-watches/{id}", deskWatch), "$.currentPrice")).isCloseTo(200.0, within(0.001));
    }
}
