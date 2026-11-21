package com.example.catalog;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class CatalogItemAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private Api seed() throws Exception {
        var api = new Api(mockMvc);
        api.create("/products", """
                {"name": "Lamp", "category": "Lighting", "price": 20, "stock": 5, "supplierNotes": "secret A"}""");
        api.create("/products", """
                {"name": "Spot", "category": "Lighting", "price": 15, "stock": 0, "supplierNotes": "secret B"}""");
        api.create("/products", """
                {"name": "Desk", "category": "Furniture", "price": 200, "stock": 1, "supplierNotes": "secret C"}""");
        return api;
    }

    @Test
    void catalogShowsOnlyPublicFields() throws Exception {
        var page = seed().get("/catalog-items?size=50").andExpect(status().isOk());

        List<Map<String, Object>> items = Api.read(page, "$.content");
        assertThat(items).hasSize(3);
        assertThat(items).allSatisfy(item -> assertThat(item.keySet())
                .containsExactlyInAnyOrder("name", "price", "inStock"));
        assertThat(items).filteredOn(item -> item.get("name").equals("Spot"))
                .singleElement().satisfies(item -> assertThat(item.get("inStock")).isEqualTo(false));
        assertThat(items).filteredOn(item -> item.get("name").equals("Lamp"))
                .singleElement().satisfies(item -> assertThat(item.get("inStock")).isEqualTo(true));
    }

    @Test
    void catalogSupportsTheCategoryFilter() throws Exception {
        List<String> names = Api.read(seed().get("/catalog-items?category=Lighting&size=50"), "$.content[*].name");
        assertThat(names).containsExactlyInAnyOrder("Lamp", "Spot");
    }

    @Test
    void productEndpointsAreUnchanged() throws Exception {
        List<String> notes = Api.read(seed().get("/products?size=50"), "$.content[*].supplierNotes");
        assertThat(notes).hasSize(3);
    }
}
