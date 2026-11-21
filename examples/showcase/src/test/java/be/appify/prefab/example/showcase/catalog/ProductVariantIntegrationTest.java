package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.example.showcase.catalog.infrastructure.http.ProductVariantResponse;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code @Parent}: variants are a nested resource of their product, {@code /products/{productId}/product-variants}. */
@IntegrationTest
class ProductVariantIntegrationTest {

    @Autowired
    ProductVariantClient variants;

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    String kettle;
    String grinder;

    @BeforeEach
    void createProducts() throws Exception {
        var coffee = categories.createCategory("Coffee");
        kettle = products.createProduct("Kettle", null, "Pressly", coffee, BigDecimal.TEN);
        grinder = products.createProduct("Grinder", null, "Brewtech", coffee, BigDecimal.TEN);
    }

    @Test
    void createProductVariant_givenProduct_canBeRetrievedUnderThatProduct() throws Exception {
        var variantId = variants.createProductVariant(kettle, "KET-BLK", "Black");

        var variant = variants.getProductVariantById(variantId, kettle);

        assertThat(variant.product().id()).isEqualTo(kettle);
        assertThat(variant.sku()).isEqualTo("KET-BLK");
        assertThat(variant.label()).isEqualTo("Black");
    }

    @Test
    void findProductVariants_givenProduct_listsOnlyItsVariants() throws Exception {
        variants.createProductVariant(kettle, "KET-WHT", "White");
        variants.createProductVariant(kettle, "KET-BLK", "Black");
        variants.createProductVariant(grinder, "GRI-STD", "Standard");

        var kettleVariants = variants.findProductVariants(Pageable.unpaged(), kettle);

        assertThat(kettleVariants.content()).extracting(ProductVariantResponse::sku)
                .containsExactly("KET-BLK", "KET-WHT");
    }
}
