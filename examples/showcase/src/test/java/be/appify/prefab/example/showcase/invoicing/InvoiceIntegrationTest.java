package be.appify.prefab.example.showcase.invoicing;

import be.appify.prefab.example.showcase.invoicing.infrastructure.http.InvoiceResponse;
import be.appify.prefab.example.showcase.ordering.OrderClient;
import be.appify.prefab.example.showcase.support.Shop;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * An aggregate created from an event ({@code @EventHandler} on a static method), updated from another event
 * ({@code @ByReference}) and changed through {@code @AsyncCommit}.
 */
@IntegrationTest
class InvoiceIntegrationTest {

    @Autowired
    InvoiceClient invoices;
    @Autowired
    OrderClient orders;
    @Autowired
    Shop shop;

    @Test
    void placeOrder_createsAnOpenInvoiceForTheTotal() throws Exception {
        var customer = shop.registerCustomer();
        var orderId = orders.createOrder(customer, shop.stockedProduct(10).id(), 2, new BigDecimal("12.50"));

        var invoice = invoiceOf(orderId);

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(invoice.amount()).isEqualByComparingTo("25.00");
        assertThat(invoice.customer().id()).isEqualTo(customer);
    }

    @Test
    void cancelOrder_voidsTheInvoice() throws Exception {
        var orderId = orders.createOrder(shop.registerCustomer(), shop.stockedProduct(10).id(), 1, BigDecimal.TEN);
        invoiceOf(orderId);

        orders.cancel(orderId);

        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(invoiceOf(orderId).status()).isEqualTo(InvoiceStatus.VOID));
    }

    @Test
    void pay_isAcceptedAtOnceAndAppliedWhenTheInvoiceHandlesItsOwnEvent() throws Exception {
        var orderId = orders.createOrder(shop.registerCustomer(), shop.stockedProduct(10).id(), 1, BigDecimal.TEN);
        var invoice = invoiceOf(orderId);

        invoices.pay(invoice.id().id());

        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(invoiceOf(orderId).status()).isEqualTo(InvoiceStatus.PAID));
    }

    private InvoiceResponse invoiceOf(String orderId) {
        return await().atMost(ofSeconds(20)).until(
                () -> invoices.findInvoices(Pageable.unpaged(), orderId).getContent().stream().findFirst(),
                found -> found.isPresent()).orElseThrow();
    }
}
