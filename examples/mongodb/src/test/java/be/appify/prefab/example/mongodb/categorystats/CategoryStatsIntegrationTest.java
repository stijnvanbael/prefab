package be.appify.prefab.example.mongodb.categorystats;

import be.appify.prefab.example.mongodb.MongoDbContainerConfiguration;
import be.appify.prefab.example.mongodb.category.Category;
import be.appify.prefab.example.mongodb.category.CategoryClient;
import be.appify.prefab.example.mongodb.product.Product;
import be.appify.prefab.example.mongodb.product.ProductClient;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
@Import(MongoDbContainerConfiguration.class)
class CategoryStatsIntegrationTest {

    @Autowired
    CategoryClient categories;
    @Autowired
    ProductClient products;
    @Autowired
    CategoryStatsClient categoryStats;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Product.class);
        mongoTemplate.dropCollection(Category.class);
        mongoTemplate.dropCollection(CategoryStats.class);
    }

    @Test
    void updateProductCountOnProductCreated() throws Exception {
        var categoryId = categories.createCategory("Electronics");
        products.createProduct("Laptop", "A laptop", BigDecimal.valueOf(999.99), "USD", categoryId);
        products.createProduct("Phone", "A phone", BigDecimal.valueOf(499.99), "USD", categoryId);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(categoryStats.findCategoryStats(Pageable.unpaged(), null).content())
                        .anySatisfy(stats -> {
                            assertThat(stats.name()).isEqualTo("Electronics");
                            assertThat(stats.totalProducts()).isEqualTo(2);
                        }));
    }
}
