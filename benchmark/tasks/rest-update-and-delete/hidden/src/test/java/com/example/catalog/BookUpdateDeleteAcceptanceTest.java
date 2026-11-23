package com.example.catalog;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class BookUpdateDeleteAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private String createBook(Api api) throws Exception {
        return api.create("/books", """
                {"title": "Dune", "author": "Frank Herbert", "isbn": "978-0441013593", "publicationYear": 1965}
                """);
    }

    @Test
    void updateChangesTitleAndAuthorOnly() throws Exception {
        var api = new Api(mockMvc);
        var id = createBook(api);

        api.put("/books/{id}", """
                {"title": "Dune Messiah", "author": "F. Herbert"}
                """, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dune Messiah"));

        api.get("/books/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dune Messiah"))
                .andExpect(jsonPath("$.author").value("F. Herbert"))
                .andExpect(jsonPath("$.isbn").value("978-0441013593"))
                .andExpect(jsonPath("$.publicationYear").value(1965));
    }

    @Test
    void deletedBookIsGone() throws Exception {
        var api = new Api(mockMvc);
        var id = createBook(api);

        api.delete("/books/{id}", id).andExpect(status().isNoContent());

        api.get("/books/{id}", id).andExpect(status().isNotFound());
    }
}
