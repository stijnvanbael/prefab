package com.example.loyalty;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class LoyaltyAccountAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void registeringACustomerOpensALoyaltyAccount() throws Exception {
        var api = new Api(mockMvc);
        var ada = api.create("/customers", "{\"name\": \"Ada\"}");
        var grace = api.create("/customers", "{\"name\": \"Grace\"}");

        Api.eventually(Duration.ofSeconds(20), () -> {
            var page = api.get("/loyalty-accounts?customer={customer}", ada).andExpect(status().isOk());
            List<Object> customers = Api.read(page, "$.content[*].customer");
            assertThat(customers).hasSize(1);
            assertThat(customers.getFirst().toString()).contains(ada);
            List<Integer> points = Api.read(page, "$.content[*].points");
            assertThat(points).containsExactly(0);
        });
        Api.eventually(Duration.ofSeconds(20), () -> {
            List<Object> customers = Api.read(api.get("/loyalty-accounts?customer={customer}", grace),
                    "$.content[*].customer");
            assertThat(customers).hasSize(1);
        });
    }

    @Test
    void loyaltyAccountsCannotBeCreatedOverRest() throws Exception {
        var status = new Api(mockMvc).post("/loyalty-accounts", "{\"points\": 1000}")
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(404, 405);
    }
}
