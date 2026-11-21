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
        var id = api.create("/books", """
                {"title": "Dune", "author": "Frank Herbert", "isbn": "978-0441013593", "publicationYear": 1965}
                """);

        api.get("/books/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Dune"))
                .andExpect(jsonPath("$.author").value("Frank Herbert"))
                .andExpect(jsonPath("$.isbn").value("978-0441013593"))
                .andExpect(jsonPath("$.publicationYear").value(1965));
    }

    @Test
    void unknownBookIsNotFound() throws Exception {
        new Api(mockMvc).get("/books/{id}", "does-not-exist").andExpect(status().isNotFound());
    }

    @Test
    void titleIsRequired() throws Exception {
        new Api(mockMvc).post("/books", """
                {"author": "Anonymous", "isbn": "123", "publicationYear": 2000}
                """).andExpect(status().isBadRequest());
    }
}
