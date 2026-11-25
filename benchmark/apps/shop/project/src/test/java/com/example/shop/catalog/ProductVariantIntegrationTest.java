package com.example.shop.catalog;

import com.example.shop.catalog.infrastructure.http.ProductVariantResponse;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code @Parent}: variants are a nested resource of their product, {@code /products/{productId}/product-variants}. */
@IntegrationTest
class ProductVariantIntegrationTest {

    @Autowired
    ProductVariantClient variants;

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    @Autowired
    MockMvc mockMvc;

    String kettle;
    String grinder;

    @BeforeEach
    void createProducts() throws Exception {
        var coffee = categories.createCategory("Coffee");
        kettle = products.createProduct("Kettle", null, "Pressly", coffee, BigDecimal.TEN);
        grinder = products.createProduct("Grinder", null, "Brewtech", coffee, BigDecimal.TEN);
    }

    @Test
    void createProductVariant_givenProduct_canBeRetrievedUnderThatProduct() throws Exception {
        var variantId = variants.createProductVariant(kettle, "KET-BLK", "Black");

        var variant = variants.getProductVariantById(variantId, kettle);

        assertThat(variant.product().id()).isEqualTo(kettle);
        assertThat(variant.sku()).isEqualTo("KET-BLK");
        assertThat(variant.label()).isEqualTo("Black");
    }

    @Test
    void findProductVariants_givenProduct_listsOnlyItsVariants() throws Exception {
        variants.createProductVariant(kettle, "KET-WHT", "White");
        variants.createProductVariant(kettle, "KET-BLK", "Black");
        variants.createProductVariant(grinder, "GRI-STD", "Standard");

        var kettleVariants = variants.findProductVariants(Pageable.unpaged(), kettle);

        assertThat(kettleVariants.content()).extracting(ProductVariantResponse::sku)
                .containsExactly("KET-BLK", "KET-WHT");
    }

    @Test
    void getProductVariant_givenOtherProduct_isNotFound() throws Exception {
        var variantId = variants.createProductVariant(kettle, "KET-BLK", "Black");

        mockMvc.perform(get("/products/{productId}/product-variants/{id}", grinder, variantId)
                        .with(user("test")))
                .andExpect(status().isNotFound());
    }
}
