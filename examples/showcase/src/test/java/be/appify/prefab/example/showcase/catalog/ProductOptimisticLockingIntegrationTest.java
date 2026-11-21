package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.example.showcase.catalog.application.ProductRepository;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Optimistic locking with Spring Data's {@code @Version}: every update increments the version, and writing a copy
 * that was read before another update fails instead of silently overwriting that update.
 */
@IntegrationTest
class ProductOptimisticLockingIntegrationTest {

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    @Autowired
    ProductRepository repository;

    @Test
    void update_incrementsTheVersion() throws Exception {
        var productId = createProduct();
        var initialVersion = products.getProductById(productId).version();

        products.reprice(productId, BigDecimal.ONE);

        assertThat(products.getProductById(productId).version()).isEqualTo(initialVersion + 1);
    }

    @Test
    void save_givenStaleCopy_failsAndKeepsTheConcurrentUpdate() throws Exception {
        var productId = createProduct();
        var staleCopy = repository.findById(productId).orElseThrow();

        products.reprice(productId, BigDecimal.ONE);

        assertThatThrownBy(() -> repository.save(staleCopy.reprice(new BigDecimal("2"))))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(products.getProductById(productId).price()).isEqualByComparingTo("1");
    }

    private String createProduct() throws Exception {
        return products.createProduct("Grinder", null, "Brewtech", categories.createCategory("Coffee"),
                BigDecimal.TEN);
    }
}
