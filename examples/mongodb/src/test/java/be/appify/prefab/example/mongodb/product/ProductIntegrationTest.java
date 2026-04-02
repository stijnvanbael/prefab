package be.appify.prefab.example.mongodb.product;

import be.appify.prefab.example.mongodb.MongoDbContainerConfiguration;
import be.appify.prefab.example.mongodb.category.Category;
import be.appify.prefab.example.mongodb.category.CategoryClient;
import be.appify.prefab.example.mongodb.categorystats.CategoryStats;
import be.appify.prefab.example.mongodb.product.infrastructure.http.ProductResponse;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Import(MongoDbContainerConfiguration.class)
class ProductIntegrationTest {

    @Autowired
    ProductClient products;
    @Autowired
    CategoryClient categories;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Product.class);
        mongoTemplate.dropCollection(Category.class);
        mongoTemplate.dropCollection(CategoryStats.class);
    }

    @Test
    void createAndRetrieveProduct() throws Exception {
        var categoryId = categories.createCategory("Gadgets");
        var productId = products.createProduct("Widget", "A useful widget", BigDecimal.valueOf(9.99), "USD", categoryId);

        var product = products.getProductById(productId);

        assertThat(product.name()).isEqualTo("Widget");
        assertThat(product.description()).isEqualTo("A useful widget");
        assertThat(product.price().amount()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
        assertThat(product.price().currency()).isEqualTo("USD");
        assertThat(product.id().id()).isEqualTo(productId);
    }

    @Test
    void listProductsWithFilter() throws Exception {
        var categoryId = categories.createCategory("Tools");
        products.createProduct("Alpha", "First product", BigDecimal.ONE, "EUR", categoryId);
        products.createProduct("Beta", "Second product", BigDecimal.TEN, "EUR", categoryId);

        var page = products.findProducts(null, "Alpha");

        assertThat(page.content()).extracting(ProductResponse::name).containsExactly("Alpha");
    }

    @Test
    void updateProduct() throws Exception {
        var categoryId = categories.createCategory("Widgets");
        var productId = products.createProduct("Old Name", "Old Desc", BigDecimal.ONE, "USD", categoryId);

        products.update(productId, "New Name", "New Desc");

        var updated = products.getProductById(productId);
        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.description()).isEqualTo("New Desc");
    }

    @Test
    void deleteProduct() throws Exception {
        var categoryId = categories.createCategory("Disposable");
        var productId = products.createProduct("Temp", "Temporary product", BigDecimal.ONE, "USD", categoryId);

        products.deleteProduct(productId);

        var page = products.findProducts(null, null);
        assertThat(page.content()).extracting(ProductResponse::name).doesNotContain("Temp");
    }

    @Test
    void createProductWithoutName_returns400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"desc\",\"amount\":10,\"currency\":\"USD\",\"category\":\"some-id\"}"))
                .andExpect(status().isBadRequest());
    }
}
