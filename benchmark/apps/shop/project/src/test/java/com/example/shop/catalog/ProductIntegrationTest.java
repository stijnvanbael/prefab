package com.example.shop.catalog;

import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@code @Create}, {@code @GetById}, {@code @Update} and {@code @Delete} on the product resource. */
@IntegrationTest
class ProductIntegrationTest {

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    String coffee;

    @BeforeEach
    void createCategory() throws Exception {
        coffee = categories.createCategory("Coffee");
    }

    @Test
    void createProduct_givenDetails_canBeRetrievedById() throws Exception {
        var productId = products.createProduct(
                "Espresso machine", "Pump driven, 15 bar", "Brewtech", coffee, new BigDecimal("349.99"));

        var product = products.getProductById(productId);

        assertThat(product.id().id()).isEqualTo(productId);
        assertThat(product.name()).isEqualTo("Espresso machine");
        assertThat(product.description()).isEqualTo("Pump driven, 15 bar");
        assertThat(product.brand()).isEqualTo("Brewtech");
        assertThat(product.category().id()).isEqualTo(coffee);
        assertThat(product.price()).isEqualByComparingTo("349.99");
        assertThat(product.image()).isNull();
    }

    @Test
    void createProduct_withoutOptionalDescription_isAccepted() throws Exception {
        var productId = products.createProduct("Milk jug", null, "Brewtech", coffee, new BigDecimal("19.95"));

        assertThat(products.getProductById(productId).description()).isNull();
    }

    @Test
    void createProduct_givenLongDescription_storesItCompletely() throws Exception {
        var description = "Rich crema. ".repeat(100);

        var productId = products.createProduct("Espresso machine", description, "Brewtech", coffee, BigDecimal.TEN);

        assertThat(products.getProductById(productId).description()).isEqualTo(description);
    }

    @Test
    void describe_givenNewNameAndDescription_replacesThem() throws Exception {
        var productId = products.createProduct("Grinder", null, "Brewtech", coffee, new BigDecimal("99.00"));

        products.describe(productId, "Burr grinder", "Conical steel burrs");

        var product = products.getProductById(productId);
        assertThat(product.name()).isEqualTo("Burr grinder");
        assertThat(product.description()).isEqualTo("Conical steel burrs");
        assertThat(product.price()).isEqualByComparingTo("99.00");
    }

    @Test
    void reprice_givenNewPrice_onlyChangesThePrice() throws Exception {
        var productId = products.createProduct("Grinder", "Conical burrs", "Brewtech", coffee, new BigDecimal("99.00"));

        products.reprice(productId, new BigDecimal("89.00"));

        var product = products.getProductById(productId);
        assertThat(product.price()).isEqualByComparingTo("89.00");
        assertThat(product.name()).isEqualTo("Grinder");
    }

    @Test
    void deleteProduct_givenExistingProduct_removesIt() throws Exception {
        var productId = products.createProduct("Tamper", null, "Brewtech", coffee, new BigDecimal("24.50"));

        products.deleteProduct(productId);

        assertThatThrownBy(() -> products.getProductById(productId))
                .hasMessageContaining("Status expected:<200> but was:<404>");
    }
}
