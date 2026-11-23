package com.example.inventory;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProductFieldsAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void newFieldsAreStored() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/products", """
                {"name": "Lamp", "price": 19.99, "description": "A desk lamp", "stock": 12}
                """);

        api.get("/products/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lamp"))
                .andExpect(jsonPath("$.description").value("A desk lamp"))
                .andExpect(jsonPath("$.stock").value(12));
    }

    @Test
    void descriptionIsOptional() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/products", """
                {"name": "Chair", "price": 49.00, "stock": 3}
                """);

        api.get("/products/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3));
    }
}
