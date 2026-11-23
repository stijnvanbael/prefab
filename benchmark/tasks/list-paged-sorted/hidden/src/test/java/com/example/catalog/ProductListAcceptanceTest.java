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
class ProductListAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void listIsPagedAndSortedByNameByDefault() throws Exception {
        var api = new Api(mockMvc);
        for (var product : List.of("Pear:3", "Apple:5", "Mango:1", "Kiwi:4", "Banana:2")) {
            var parts = product.split(":");
            api.create("/products", "{\"name\": \"%s\", \"category\": \"Fruit\", \"price\": %s}".formatted(parts[0], parts[1]));
        }

        var firstPage = api.get("/products?page=0&size=2").andExpect(status().isOk());
        List<String> firstNames = Api.read(firstPage, "$.content[*].name");
        assertThat(firstNames).containsExactly("Apple", "Banana");
        List<Integer> total = Api.read(firstPage, "$..totalElements");
        assertThat(total).first().isEqualTo(5);

        List<String> secondNames = Api.read(api.get("/products?page=1&size=2"), "$.content[*].name");
        assertThat(secondNames).containsExactly("Kiwi", "Mango");
    }

    @Test
    void explicitSortWins() throws Exception {
        var api = new Api(mockMvc);
        for (var product : List.of("Pear:3", "Apple:5", "Mango:1")) {
            var parts = product.split(":");
            api.create("/products", "{\"name\": \"%s\", \"category\": \"Fruit\", \"price\": %s}".formatted(parts[0], parts[1]));
        }

        List<String> names = Api.read(api.get("/products?sort=price,desc&size=10"), "$.content[*].name");
        assertThat(names).containsExactly("Apple", "Pear", "Mango");
    }
}
