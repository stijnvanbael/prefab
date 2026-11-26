package com.example.shop.customer;

import be.appify.prefab.test.IntegrationTest;
import benchmark.support.Api;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class LoyaltyTierAcceptanceTest {
    @Autowired
    MockMvc mockMvc;

    private String register(Api api) throws Exception {
        return api.create("/customers", """
                {"firstName": "Ada", "lastName": "Lovelace", "email": "ada-%s@example.com"}
                """.formatted(UUID.randomUUID()));
    }

    @Test
    void newCustomersStartAtBronze() throws Exception {
        var api = new Api(mockMvc).with(user("clerk"));
        var id = register(api);

        api.get("/customers/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyaltyTier").value("BRONZE"))
                .andExpect(jsonPath("$.firstName").value("Ada"));
    }

    @Test
    void administratorsChangeTheTier() throws Exception {
        var admin = new Api(mockMvc).with(user("admin").roles("ADMIN"));
        var clerk = new Api(mockMvc).with(user("clerk").roles("CUSTOMER"));
        var id = register(clerk);

        clerk.put("/customers/{id}/loyalty-tier", "{\"loyaltyTier\": \"GOLD\"}", id).andExpect(status().isForbidden());
        admin.put("/customers/{id}/loyalty-tier", "{\"loyaltyTier\": \"GOLD\"}", id).andExpect(status().isOk());

        admin.get("/customers/{id}", id).andExpect(jsonPath("$.loyaltyTier").value("GOLD"));
    }

    @Test
    void existingCustomersBecomeBronzeAndTheColumnIsRequired() throws Exception {
        var sql = new StringBuilder();
        for (var script : new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql")) {
            sql.append(script.getContentAsString(StandardCharsets.UTF_8)).append(";\n");
        }
        var migrations = sql.toString().toLowerCase(Locale.ROOT).replace("\"", "").replaceAll("\\s+", " ");
        var statements = migrations.split(";");

        assertThat(migrations).contains("add column loyalty_tier");
        assertThat(migrations)
                .as("the released inline foreign key is not re-added as a table constraint")
                .doesNotContain("add constraint customer_order_history_entry_customer_order_history_fk");
        var backfilled = Pattern.compile("add column loyalty_tier [^;]*default '?bronze'?").matcher(migrations).find()
                || Pattern.compile("update customer set loyalty_tier ?= ?'bronze'").matcher(migrations).find();
        assertThat(backfilled).as("existing customers get BRONZE (a column default or an UPDATE)").isTrue();
        var required = false;
        for (var statement : statements) {
            if (statement.contains("loyalty_tier") && statement.contains("not null")) {
                required = true;
            }
        }
        assertThat(required).as("loyalty_tier is NOT NULL").isTrue();
    }
}
