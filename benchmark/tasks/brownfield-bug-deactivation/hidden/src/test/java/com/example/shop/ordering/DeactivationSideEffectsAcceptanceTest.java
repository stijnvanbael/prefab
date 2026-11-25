package com.example.shop.ordering;

import be.appify.prefab.test.IntegrationTest;
import com.example.shop.customer.CustomerClient;
import com.example.shop.inventory.StockLevelClient;
import com.example.shop.invoicing.InvoiceClient;
import com.example.shop.invoicing.InvoiceStatus;
import com.example.shop.support.Shop;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class DeactivationSideEffectsAcceptanceTest {
    @Autowired
    CustomerClient customers;
    @Autowired
    OrderClient orders;
    @Autowired
    InvoiceClient invoices;
    @Autowired
    StockLevelClient stockLevels;
    @Autowired
    Shop shop;

    @Test
    void deactivatingACustomerVoidsTheInvoicesAndReleasesTheStockOfTheirOrders() throws Exception {
        var leaving = shop.registerCustomer();
        var staying = shop.registerCustomer();
        var stocked = shop.stockedProduct(10);
        var first = orders.createOrder(leaving, stocked.id(), 2, BigDecimal.TEN);
        var second = orders.createOrder(leaving, stocked.id(), 3, BigDecimal.ONE);
        var unrelated = orders.createOrder(staying, stocked.id(), 1, BigDecimal.TEN);
        await().atMost(ofSeconds(20)).untilAsserted(() -> {
            assertThat(stockLevels.getStockLevelById(stocked.stockLevelId()).reserved()).isEqualTo(6);
            assertThat(invoiceStatus(first)).isEqualTo(InvoiceStatus.OPEN);
            assertThat(invoiceStatus(second)).isEqualTo(InvoiceStatus.OPEN);
            assertThat(invoiceStatus(unrelated)).isEqualTo(InvoiceStatus.OPEN);
        });

        customers.deactivate(leaving);

        await().atMost(ofSeconds(20)).untilAsserted(() -> {
            assertThat(orders.getOrderById(first).status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(orders.getOrderById(second).status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(invoiceStatus(first)).isEqualTo(InvoiceStatus.VOID);
            assertThat(invoiceStatus(second)).isEqualTo(InvoiceStatus.VOID);
            var stockLevel = stockLevels.getStockLevelById(stocked.stockLevelId());
            assertThat(stockLevel.reserved()).isEqualTo(1);
            assertThat(stockLevel.available()).isEqualTo(9);
        });
        assertThat(orders.getOrderById(unrelated).status()).isEqualTo(OrderStatus.PLACED);
        assertThat(invoiceStatus(unrelated)).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void deactivatingTwiceDoesNotReleaseTheStockTwice() throws Exception {
        var customer = shop.registerCustomer();
        var stocked = shop.stockedProduct(10);
        orders.createOrder(customer, stocked.id(), 4, BigDecimal.TEN);
        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(stockLevels.getStockLevelById(stocked.stockLevelId()).reserved()).isEqualTo(4));

        customers.deactivate(customer);
        customers.deactivate(customer);

        await().atMost(ofSeconds(20)).untilAsserted(() -> {
            var stockLevel = stockLevels.getStockLevelById(stocked.stockLevelId());
            assertThat(stockLevel.reserved()).isZero();
            assertThat(stockLevel.available()).isEqualTo(10);
        });
        Thread.sleep(500);
        assertThat(stockLevels.getStockLevelById(stocked.stockLevelId()).available()).isEqualTo(10);
    }

    private InvoiceStatus invoiceStatus(String orderId) throws Exception {
        return invoices.findInvoices(Pageable.unpaged(), orderId).getContent().stream().findFirst()
                .orElseThrow(() -> new AssertionError("no invoice for order " + orderId)).status();
    }
}
