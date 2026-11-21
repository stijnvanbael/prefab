package com.example.catalog;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProductFilterAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api seed() throws Exception {
        var api = new Api(mockMvc);
        api.create("/products", "{\"name\": \"Laptop stand\", \"category\": \"Office\", \"price\": 30}");
        api.create("/products", "{\"name\": \"Gaming laptop\", \"category\": \"Electronics\", \"price\": 1500}");
        api.create("/products", "{\"name\": \"Java book\", \"category\": \"Books\", \"price\": 40}");
        api.create("/products", "{\"name\": \"Kotlin book\", \"category\": \"Ebooks\", \"price\": 20}");
        return api;
    }

    @Test
    void filtersByCategoryIgnoringCase() throws Exception {
        List<String> names = Api.read(seed().get("/products?category=books&size=50").andExpect(status().isOk()),
                "$.content[*].name");
        assertThat(names).containsExactly("Java book");
    }

    @Test
    void filtersByNameContainingIgnoringCase() throws Exception {
        List<String> names = Api.read(seed().get("/products?name=LAP&size=50"), "$.content[*].name");
        assertThat(names).containsExactlyInAnyOrder("Laptop stand", "Gaming laptop");
    }

    @Test
    void combinesFilters() throws Exception {
        List<String> names = Api.read(seed().get("/products?name=lap&category=office&size=50"), "$.content[*].name");
        assertThat(names).containsExactly("Laptop stand");
    }

    @Test
    void returnsEverythingWithoutFilters() throws Exception {
        List<String> names = Api.read(seed().get("/products?size=50"), "$.content[*].name");
        assertThat(names).hasSize(4);
    }
}
