package com.example.shop.catalog;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProductReviewAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    Api admin;
    Api shopper;
    String customer;
    String espresso;
    String grinder;

    @BeforeEach
    void createCatalog() throws Exception {
        admin = new Api(mockMvc).with(user("admin").roles("ADMIN"));
        shopper = new Api(mockMvc).with(user("shopper").roles("CUSTOMER"));
        var category = admin.create("/categories", "{\"name\": \"Coffee\"}");
        espresso = product(category, "Espresso machine");
        grinder = product(category, "Grinder");
        customer = shopper.create("/customers", """
                {"firstName": "Ada", "lastName": "Lovelace", "email": "ada-%s@example.com"}
                """.formatted(UUID.randomUUID()));
    }

    private String product(String category, String name) throws Exception {
        return admin.create("/products", """
                {"name": "%s", "brand": "Brewtech", "category": "%s", "price": 99}
                """.formatted(name, category));
    }

    private String review(int rating, String comment) {
        return """
                {"customer": "%s", "rating": %d, "comment": %s}
                """.formatted(customer, rating, comment == null ? "null" : "\"" + comment + "\"");
    }

    @Test
    void reviewsUpdateTheRatingSummaryOfTheirProductOnly() throws Exception {
        shopper.create("/products/{id}/reviews", review(5, "Great crema"), espresso);
        shopper.create("/products/{id}/reviews", review(4, null), espresso);

        Api.eventually(Duration.ofSeconds(20), () -> {
            var product = shopper.get("/products/{id}", espresso).andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewCount").value(2));
            assertThat(Api.number(product, "$.averageRating")).isCloseTo(4.5, within(0.001));
        });
        var untouched = shopper.get("/products/{id}", grinder).andExpect(jsonPath("$.reviewCount").value(0));
        assertThat(untouched.andReturn().getResponse().getContentAsString())
                .doesNotContainPattern("\"averageRating\"\\s*:\\s*[0-9]");
    }

    @Test
    void reviewsAreListedPerProduct() throws Exception {
        var first = shopper.create("/products/{id}/reviews", review(3, "Fine"), espresso);
        shopper.create("/products/{id}/reviews", review(2, null), grinder);

        var page = shopper.get("/products/{id}/reviews", espresso).andExpect(status().isOk());
        List<String> ids = Api.read(page, "$.content[*].id");
        List<Integer> ratings = Api.read(page, "$.content[*].rating");
        List<String> comments = Api.read(page, "$.content[*].comment");
        assertThat(ids).containsExactly(first);
        assertThat(ratings).containsExactly(3);
        assertThat(comments).containsExactly("Fine");
    }

    @Test
    void ratingsOutsideOneToFiveAndAnonymousReviewsAreRejected() throws Exception {
        shopper.post("/products/{id}/reviews", review(0, null), espresso).andExpect(status().isBadRequest());
        shopper.post("/products/{id}/reviews", review(6, null), espresso).andExpect(status().isBadRequest());
        new Api(mockMvc).post("/products/{id}/reviews", review(4, null), espresso)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void existingProductsStartWithoutReviews() throws Exception {
        var sql = new StringBuilder();
        for (var script : new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql")) {
            sql.append(script.getContentAsString(StandardCharsets.UTF_8)).append(";\n");
        }
        var migrations = sql.toString().toLowerCase(Locale.ROOT).replace("\"", "").replaceAll("\\s+", " ");

        for (var statement : migrations.split(";")) {
            if (statement.contains("alter table product ") && statement.contains("add column")
                    && statement.contains("not null")) {
                assertThat(statement).as("a required column added to the product table needs a default")
                        .contains("default");
            }
        }
    }
}
