package be.appify.prefab.example.showcase;

import be.appify.prefab.example.showcase.catalog.CategoryClient;
import be.appify.prefab.example.showcase.catalog.ProductClient;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers endpoint security and the generated API documentation served by the showcase. */
@IntegrationTest
class ShowcaseSecurityAndDocumentationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    @Test
    void defaultSecurityRequiresAuthenticationAndDisabledEndpointNeedsNoRole() throws Exception {
        var categoryId = categories.createCategory("Coffee");
        var productId = products.createProduct(
                "Espresso machine", null, "Brewtech", categoryId, new BigDecimal("349.99"));

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/products/{id}", productId).with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/categories").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    void catalogDeletionIsAllowedForAdminsAndForbiddenForCustomers() throws Exception {
        var categoryId = categories.createCategory("Coffee");
        var productId = products.createProduct(
                "Espresso machine", null, "Brewtech", categoryId, new BigDecimal("349.99"));

        mockMvc.perform(delete("/products/{id}", productId).with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/products/{id}", productId)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void openApiAndAsyncApiDocumentsAreServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs").with(user("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/products']").exists())
                .andExpect(jsonPath("$.paths['/customers/{customerId}/order-history']").exists());
        mockMvc.perform(get("/swagger-ui/index.html").with(user("reader")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/async-api/asyncapi.json").with(user("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asyncapi").exists())
                .andExpect(jsonPath("$.channels['showcase.order']").exists());
    }
}
