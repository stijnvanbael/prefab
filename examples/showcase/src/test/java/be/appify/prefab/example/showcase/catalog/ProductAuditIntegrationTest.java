package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Audit fields ({@code @CreatedAt}, {@code @CreatedBy}, {@code @LastModifiedAt}, {@code @LastModifiedBy}, grouped in
 * {@code AuditInfo}) are maintained by Prefab from the authenticated principal.
 */
@IntegrationTest
class ProductAuditIntegrationTest {

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    @Test
    void createProduct_recordsWhenAndByWhom() throws Exception {
        var before = Instant.now();

        var productId = products.createProduct("Grinder", null, "Brewtech", categories.createCategory("Coffee"),
                BigDecimal.TEN);

        var audit = products.getProductById(productId).audit();
        assertThat(audit.createdAt()).isAfterOrEqualTo(before);
        assertThat(audit.createdBy()).isEqualTo("test");
        assertThat(audit.lastModifiedAt()).isCloseTo(audit.createdAt(), within(1, ChronoUnit.SECONDS));
        assertThat(audit.lastModifiedBy()).isEqualTo("test");
    }

    @Test
    void update_onlyChangesTheModificationFields() throws Exception {
        var productId = products.createProduct("Grinder", null, "Brewtech", categories.createCategory("Coffee"),
                BigDecimal.TEN);
        var created = products.getProductById(productId).audit();

        products.reprice(productId, BigDecimal.ONE);

        var updated = products.getProductById(productId).audit();
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
        assertThat(updated.createdBy()).isEqualTo(created.createdBy());
        assertThat(updated.lastModifiedAt()).isAfter(created.lastModifiedAt());
        assertThat(updated.lastModifiedBy()).isEqualTo("test");
    }
}
