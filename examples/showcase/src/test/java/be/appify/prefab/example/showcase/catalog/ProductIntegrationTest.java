package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ProductIntegrationTest {

    @Autowired
    ProductClient products;

    @Test
    void createProduct_givenName_canBeRetrievedById() throws Exception {
        var productId = products.createProduct("Espresso machine");

        var product = products.getProductById(productId);

        assertThat(product.name()).isEqualTo("Espresso machine");
    }
}
