package be.appify.prefab.example.kafka.product;

import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ProductIntegrationTest {

    @Autowired
    ProductClient productClient;

    @Test
    void createProductWithJsonbDetails() throws Exception {
        var details = new ProductDetails("A high-performance laptop", "Electronics");
        var productId = productClient.createProduct("Laptop Pro", details);

        var product = productClient.getProductById(productId);

        assertThat(product.name()).isEqualTo("Laptop Pro");
        assertThat(product.details()).isNotNull();
        assertThat(product.details().category()).isEqualTo("Electronics");
        assertThat(product.details().description()).isEqualTo("A high-performance laptop");
    }

    @Test
    void createProductWithJsonbTagsList() throws Exception {
        var details = new ProductDetails("A smartphone", "Mobile");
        var productId = productClient.createProduct("Phone X", details);

        productClient.addTag(productId, new ProductTag("color", "black"));
        productClient.addTag(productId, new ProductTag("storage", "256GB"));

        var product = productClient.getProductById(productId);

        assertThat(product.tags()).hasSize(2);
        assertThat(product.tags()).extracting(ProductTag::name).containsExactlyInAnyOrder("color", "storage");
        assertThat(product.tags()).extracting(ProductTag::value).containsExactlyInAnyOrder("black", "256GB");
    }

    @Test
    void queryProductsByCategory() throws Exception {
        var electronics = new ProductDetails("A monitor", "Electronics");
        productClient.createProduct("Monitor", electronics);
        var home = new ProductDetails("A table", "HomeOffice");
        productClient.createProduct("Desk", home);

        var products = productClient.findProducts(Pageable.unpaged());

        assertThat(products.getContent()).extracting(p -> p.details().category())
                .containsExactlyInAnyOrder("Electronics", "HomeOffice");
    }
}
