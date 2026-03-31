package be.appify.prefab.example.mongodb.product;

import be.appify.prefab.example.mongodb.MongoDbContainerConfiguration;
import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(MongoDbContainerConfiguration.class)
class ProductIntegrationTest {

    @Autowired
    ProductClient products;

    @Test
    void createAndRetrieveProduct() {
        var productId = products.createProduct("Widget", "A useful widget");

        var product = products.getProductById(productId);

        assertThat(product.name()).isEqualTo("Widget");
        assertThat(product.description()).isEqualTo("A useful widget");
        assertThat(product.id().id()).isEqualTo(productId);
    }

    @Test
    void listProducts() {
        products.createProduct("Alpha", "First product");
        products.createProduct("Beta", "Second product");

        var page = products.getProducts();

        assertThat(page.content()).extracting(Product::name).contains("Alpha", "Beta");
    }
}
