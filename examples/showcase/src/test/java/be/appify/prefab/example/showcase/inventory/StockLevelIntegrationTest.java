package be.appify.prefab.example.showcase.inventory;

import be.appify.prefab.example.showcase.ordering.OrderClient;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * {@code @EventHandler} that updates another aggregate: placing an order reserves stock, cancelling it releases the
 * stock. The handler is tuned with {@code @EventHandlerConfig}.
 */
@IntegrationTest
class StockLevelIntegrationTest {

    @Autowired
    StockLevelClient stockLevels;
    @Autowired
    OrderClient orders;
    @Autowired
    Shop shop;

    @Test
    void placeOrder_reservesTheOrderedQuantity() throws Exception {
        var customer = shop.registerCustomer();
        var stocked = shop.stockedProduct(10);
        var stockLevelId = stocked.stockLevelId();

        orders.createOrder(customer, stocked.id(), 3, BigDecimal.TEN);

        await().atMost(ofSeconds(20)).untilAsserted(() -> {
            var stockLevel = stockLevels.getStockLevelById(stockLevelId);
            assertThat(stockLevel.available()).isEqualTo(7);
            assertThat(stockLevel.reserved()).isEqualTo(3);
        });
    }

    @Test
    void cancelOrder_releasesTheReservedQuantity() throws Exception {
        var customer = shop.registerCustomer();
        var stocked = shop.stockedProduct(10);
        var stockLevelId = stocked.stockLevelId();
        var orderId = orders.createOrder(customer, stocked.id(), 3, BigDecimal.TEN);
        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(stockLevels.getStockLevelById(stockLevelId).reserved()).isEqualTo(3));

        orders.cancel(orderId);

        await().atMost(ofSeconds(20)).untilAsserted(() -> {
            var stockLevel = stockLevels.getStockLevelById(stockLevelId);
            assertThat(stockLevel.available()).isEqualTo(10);
            assertThat(stockLevel.reserved()).isZero();
        });
    }

    @Test
    void placeOrder_forAnotherProduct_leavesTheStockAlone() throws Exception {
        var customer = shop.registerCustomer();
        var stocked = shop.stockedProduct(10);
        var other = shop.stockedProduct(10);
        var stockLevelId = stocked.stockLevelId();
        var otherStockLevelId = other.stockLevelId();

        orders.createOrder(customer, other.id(), 4, BigDecimal.TEN);

        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(stockLevels.getStockLevelById(otherStockLevelId).reserved()).isEqualTo(4));
        assertThat(stockLevels.getStockLevelById(stockLevelId).reserved()).isZero();
    }
}
