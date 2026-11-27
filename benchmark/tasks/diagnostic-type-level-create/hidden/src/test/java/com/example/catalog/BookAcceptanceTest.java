package com.example.catalog;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class BookAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void createdBookCanBeRetrieved() throws Exception {
        var api = new Api(mockMvc);
        var id = api.create("/books", "{\"title\": \"Dune\", \"author\": \"Frank Herbert\"}");

        api.get("/books/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Dune"))
                .andExpect(jsonPath("$.author").value("Frank Herbert"));
    }
}
