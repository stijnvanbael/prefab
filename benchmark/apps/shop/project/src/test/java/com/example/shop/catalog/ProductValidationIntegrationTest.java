package com.example.shop.catalog;

import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Jakarta Validation constraints on {@code @Create} and {@code @Update} parameters, and Prefab's {@code @ContentType}
 * and {@code @FileSize} on uploaded files: an invalid request is rejected with 400 Bad Request and changes nothing.
 */
@IntegrationTest
class ProductValidationIntegrationTest {

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    String coffee;

    @BeforeEach
    void createCategory() throws Exception {
        coffee = categories.createCategory("Coffee");
    }

    @Test
    void createProduct_givenBlankName_isRejected() {
        assertBadRequest(() -> products.createProduct(" ", null, "Brewtech", coffee, BigDecimal.TEN));
    }

    @Test
    void createProduct_givenTooLongName_isRejected() {
        assertBadRequest(() -> products.createProduct("x".repeat(256), null, "Brewtech", coffee, BigDecimal.TEN));
    }

    @Test
    void createProduct_givenTooLongDescription_isRejected() {
        var description = "x".repeat(Product.MAX_DESCRIPTION_LENGTH + 1);

        assertBadRequest(() -> products.createProduct("Grinder", description, "Brewtech", coffee, BigDecimal.TEN));
    }

    @Test
    void createProduct_givenNegativePrice_isRejected() {
        assertBadRequest(() -> products.createProduct("Grinder", null, "Brewtech", coffee, new BigDecimal("-1")));
    }

    @Test
    void createProduct_withoutCategory_isRejected() {
        assertBadRequest(() -> products.createProduct("Grinder", null, "Brewtech", null, BigDecimal.TEN));
    }

    @Test
    void createCategory_givenBlankName_isRejected() {
        assertBadRequest(() -> categories.createCategory(""));
    }

    @Test
    void reprice_givenZeroPrice_isRejectedAndKeepsThePrice() throws Exception {
        var productId = products.createProduct("Grinder", null, "Brewtech", coffee, BigDecimal.TEN);

        assertBadRequest(() -> products.reprice(productId, BigDecimal.ZERO));

        assertThat(products.getProductById(productId).price()).isEqualByComparingTo("10");
    }

    @Test
    void changeImage_givenUnsupportedContentType_isRejected() throws Exception {
        var productId = products.createProduct("Grinder", null, "Brewtech", coffee, BigDecimal.TEN);
        var pdf = new MockMultipartFile("image", "manual.pdf", "application/pdf", new byte[] {1, 2, 3});

        assertBadRequest(() -> products.changeImage(productId, pdf));

        assertThat(products.getProductById(productId).image()).isNull();
    }

    @Test
    void changeImage_givenTooLargeFile_isRejected() throws Exception {
        var productId = products.createProduct("Grinder", null, "Brewtech", coffee, BigDecimal.TEN);
        var huge = new MockMultipartFile("image", "huge.png", "image/png", new byte[Product.MAX_IMAGE_SIZE + 1]);

        assertBadRequest(() -> products.changeImage(productId, huge));

        assertThat(products.getProductById(productId).image()).isNull();
    }

    private static void assertBadRequest(ThrowingCallable request) {
        assertThatThrownBy(request).hasMessageContaining("but was:<400>");
    }
}
