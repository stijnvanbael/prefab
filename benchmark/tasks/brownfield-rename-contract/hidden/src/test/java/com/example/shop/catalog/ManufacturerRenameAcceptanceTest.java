package com.example.shop.catalog;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ManufacturerRenameAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    Api admin;
    String category;

    @BeforeEach
    void createCategory() throws Exception {
        admin = new Api(mockMvc).with(user("admin").roles("ADMIN"));
        category = admin.create("/categories", "{\"name\": \"Coffee\"}");
    }

    private String product(String name, String manufacturer) throws Exception {
        return admin.create("/products", """
                {"name": "%s", "manufacturer": "%s", "category": "%s", "price": 10}
                """.formatted(name, manufacturer, category));
    }

    @Test
    void productsHaveAManufacturerAndStillShowItAsBrand() throws Exception {
        var id = product("Espresso machine", "Brewtech");

        admin.get("/products/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manufacturer").value("Brewtech"))
                .andExpect(jsonPath("$.brand").value("Brewtech"));
    }

    @Test
    void productsAreFilteredByManufacturer() throws Exception {
        var unique = "Maker" + System.nanoTime();
        var mine = product("Grinder", unique);
        product("Kettle", "Other" + System.nanoTime());

        List<String> byManufacturer = Api.read(admin.get("/products?manufacturer={m}", unique)
                .andExpect(status().isOk()), "$.content[*].id");
        List<String> listedManufacturers = Api.read(admin.get("/products?manufacturer={m}", unique),
                "$.content[*].manufacturer");

        assertThat(byManufacturer).containsExactly(mine);
        assertThat(listedManufacturers).containsExactly(unique);
    }

    @Test
    void theStoredColumnIsRenamedNotReplaced() throws Exception {
        var sql = new StringBuilder();
        for (var script : new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql")) {
            sql.append(script.getContentAsString(StandardCharsets.UTF_8)).append('\n');
        }
        var migrations = sql.toString().toLowerCase(Locale.ROOT).replace("\"", "").replaceAll("\\s+", " ");

        assertThat(migrations).contains("rename column brand to manufacturer");
        assertThat(migrations).doesNotContain("drop column brand");
        assertThat(migrations).doesNotContain("add column manufacturer");
    }
}
