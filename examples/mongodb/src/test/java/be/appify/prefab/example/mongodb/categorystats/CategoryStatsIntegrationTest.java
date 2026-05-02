package be.appify.prefab.example.mongodb.categorystats;

import be.appify.prefab.core.outbox.OutboxRelayService;
import be.appify.prefab.example.mongodb.category.CategoryClient;
import be.appify.prefab.example.mongodb.product.ProductClient;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class CategoryStatsIntegrationTest {

    @Autowired
    CategoryClient categories;
    @Autowired
    ProductClient products;
    @Autowired
    CategoryStatsClient categoryStats;
    @Autowired
    OutboxRelayService outboxRelayService;

    @Test
    void updateProductCountOnProductCreated() throws Exception {
        var categoryId = categories.createCategory("Electronics");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(categoryStats.findCategoryStatses(Pageable.unpaged(), (String) null).content())
                        .anySatisfy(stats -> assertThat(stats.name()).isEqualTo("Electronics")));

        products.createProduct("Laptop", "A laptop", BigDecimal.valueOf(999.99), "USD", categoryId);
        products.createProduct("Phone", "A phone", BigDecimal.valueOf(499.99), "USD", categoryId);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(categoryStats.findCategoryStatses(Pageable.unpaged(), (String) null).content())
                        .anySatisfy(stats -> {
                            assertThat(stats.name()).isEqualTo("Electronics");
                            assertThat(stats.totalProducts()).isEqualTo(2);
                        }));
    }
}
