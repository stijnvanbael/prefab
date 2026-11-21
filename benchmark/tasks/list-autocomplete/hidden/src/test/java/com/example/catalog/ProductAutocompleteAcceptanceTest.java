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
class ProductAutocompleteAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void suggestsNamesStartingWithQuery() throws Exception {
        var api = new Api(mockMvc);
        for (var name : List.of("Laptop", "laptop bag", "Lamp", "Desk", "Laptop")) {
            api.create("/products", "{\"name\": \"%s\", \"category\": \"Office\", \"price\": 10}".formatted(name));
        }

        List<String> suggestions = Api.read(api.get("/products/name/autocomplete?query=lap").andExpect(status().isOk()), "$");
        assertThat(suggestions).containsExactlyInAnyOrder("Laptop", "laptop bag");
    }

    @Test
    void doesNotSuggestNamesThatOnlyContainTheQuery() throws Exception {
        var api = new Api(mockMvc);
        api.create("/products", "{\"name\": \"Desk lamp\", \"category\": \"Office\", \"price\": 10}");

        List<String> suggestions = Api.read(api.get("/products/name/autocomplete?query=lamp"), "$");
        assertThat(suggestions).isEmpty();
    }
}
