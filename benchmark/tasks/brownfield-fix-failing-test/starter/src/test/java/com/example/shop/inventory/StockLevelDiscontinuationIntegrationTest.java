package com.example.shop.inventory;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.test.IntegrationTest;
import com.example.shop.catalog.ProductClient;
import com.example.shop.catalog.ProductDiscontinued;
import com.example.shop.inventory.application.StockLevelService;
import com.example.shop.support.Shop;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

/** Discontinuing a product writes off its remaining stock. */
@IntegrationTest
class StockLevelDiscontinuationIntegrationTest {

    @Autowired
    ProductClient products;
    @Autowired
    StockLevelClient stockLevels;
    @Autowired
    StockLevelService stockLevelService;
    @Autowired
    Shop shop;

    @Test
    void discontinueProduct_writesOffTheAvailableStock() throws Exception {
        var stocked = shop.stockedProduct(10);

        products.discontinue(stocked.id());

        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(stockLevels.getStockLevelById(stocked.stockLevelId()).available()).isZero());
    }

    @Test
    void discontinueProduct_withoutStockLevel_isHandledWithoutError() {
        var event = new ProductDiscontinued(Reference.fromId(UUID.randomUUID().toString()));

        assertThatCode(() -> stockLevelService.onProductDiscontinued(event)).doesNotThrowAnyException();
    }
}
