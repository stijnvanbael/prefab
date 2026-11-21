package com.example.ordering;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class OrderActionsAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void openOrderCanBeCancelled() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/orders", "{\"customer\": \"Ada\"}");

        api.postEmpty("/orders/{id}/cancel", id).andExpect(status().isOk());

        api.get("/orders/{id}", id).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shippedOrderCannotBeCancelled() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/orders", "{\"customer\": \"Ada\"}");

        api.postEmpty("/orders/{id}/ship", id).andExpect(status().isOk());
        api.postEmpty("/orders/{id}/cancel", id).andExpect(status().isConflict());

        api.get("/orders/{id}", id).andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}
