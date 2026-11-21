package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.example.showcase.capability.RequiresCapability;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static be.appify.prefab.example.showcase.capability.Capability.BINARY_DOWNLOAD;
import static org.assertj.core.api.Assertions.assertThat;

/** A {@code Binary} field: uploaded as multipart through {@code @Update}, served by {@code @Download}. */
@IntegrationTest
@RequiresCapability(BINARY_DOWNLOAD)
class ProductImageIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

    @Autowired
    ProductClient products;

    @Autowired
    CategoryClient categories;

    @Test
    void downloadImage_returnsTheUploadedImage() throws Exception {
        var productId = products.createProduct("Grinder", null, "Brewtech", categories.createCategory("Coffee"),
                BigDecimal.TEN);

        products.changeImage(productId, new MockMultipartFile("image", "grinder.png", "image/png", PNG));

        assertThat(products.downloadImage(productId)).isEqualTo(PNG);
        var image = products.getProductById(productId).image();
        assertThat(image.name()).isEqualTo("grinder.png");
        assertThat(image.contentType()).isEqualTo("image/png");
    }
}
