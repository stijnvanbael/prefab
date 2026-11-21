package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.catalog.application.ProductRepository;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code @RepositoryMixin}: the generated repository also offers the queries of {@link ProductRepositoryMixin}. */
@IntegrationTest
class ProductRepositoryMixinIntegrationTest {

    @Autowired
    ProductRepository repository;

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    @Test
    void countByCategory_countsOnlyProductsInThatCategory() throws Exception {
        var coffee = categories.createCategory("Coffee");
        var tea = categories.createCategory("Tea");
        products.createProduct("Grinder", null, "Brewtech", coffee, BigDecimal.TEN);
        products.createProduct("French press", null, "Pressly", coffee, BigDecimal.TEN);
        products.createProduct("Kettle", null, "Pressly", tea, BigDecimal.TEN);

        assertThat(repository.countByCategory(Reference.fromId(coffee))).isEqualTo(2);
        assertThat(repository.countByCategory(Reference.fromId(tea))).isEqualTo(1);
    }
}
