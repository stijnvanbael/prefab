package be.appify.prefab.example.sale;

import be.appify.prefab.example.IntegrationTest;
import be.appify.prefab.example.sale.application.CreateCustomerRequest;
import be.appify.prefab.example.sale.application.CreateGiftVoucherRequest;
import be.appify.prefab.example.sale.application.CreateSaleRequest;
import be.appify.prefab.example.sale.application.SaleAddCustomerRequest;
import be.appify.prefab.example.sale.application.SaleAddItemRequest;
import be.appify.prefab.example.sale.application.SaleAddPaymentRequest;
import be.appify.prefab.example.sale.infrastructure.persistence.InvoiceCrudRepository;
import be.appify.prefab.example.sale.infrastructure.persistence.SaleCrudRepository;
import be.appify.prefab.test.kafka.KafkaContainerSupport;
import be.appify.prefab.test.kafka.TestConsumer;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import static be.appify.prefab.example.sale.PaymentMethod.GIFT_VOUCHER;
import static be.appify.prefab.test.kafka.asserts.KafkaAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@IntegrationTest
class SaleIntegrationTest implements KafkaContainerSupport {
    @Autowired
    private SaleCrudRepository saleRepository;
    @TestConsumer(topic = "${kafka.topics.sale.name}")
    private Consumer<String, SaleCompleted> saleConsumer;
    @Autowired
    private InvoiceCrudRepository invoiceRepository;
    @Autowired
    SaleFixture sales;
    @Autowired
    CustomerFixture customers;
    @Autowired
    GiftVoucherFixture giftVouchers;
    @Autowired
    InvoiceFixture invoices;

    @BeforeEach
    void setup() {
        saleRepository.deleteAll();
        invoiceRepository.deleteAll();
    }

    @Test
    void simpleSale() throws Exception {
        var saleId = sales.givenSaleCreated(new CreateSaleRequest(SaleType.REGULAR));
        addItem(saleId);
        addPayment(saleId);

        var sale = sales.getSaleById(saleId);
        assertThat(sale.items()).hasSize(1);
        assertThat(sale.payments()).hasSize(1);
        assertThat(sale.returned()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sale.state()).isEqualTo(State.COMPLETED);

        await().untilAsserted(() ->
                assertThat(saleConsumer).hasReceivedMessagesWithin(5, TimeUnit.SECONDS)
                        .where(records ->
                                records.anySatisfy(event ->
                                        assertThat(event).isInstanceOf(SaleCompleted.class))));
    }

    @Test
    void saleWithCustomer() throws Exception {
        var customerId = createCustomer();

        var saleId = sales.createSale(new CreateSaleRequest(SaleType.REGULAR));
        addItem(saleId);
        addCustomer(saleId, customerId);
        addPayment(saleId);

        var sale = sales.getSaleById(saleId);
        assertThat(sale.customer().id()).isEqualTo(customerId);
    }

    @Test
    void saleWithGiftVoucherPayment() throws Exception {
        var giftVoucherId = createGiftVoucher(25.0);
        var saleId = sales.createSale(new CreateSaleRequest(SaleType.REGULAR));
        addItem(saleId);
        addGiftVoucherPayment(saleId, giftVoucherId);

        var sale = sales.getSaleById(saleId);
        assertThat(sale.payments()).hasSize(1);
        assertThat(sale.payments().getFirst().method()).isEqualTo(GIFT_VOUCHER);

        var giftVoucher = giftVouchers.getGiftVoucherById(giftVoucherId);
        assertThat(giftVoucher.remainingValue()).isEqualByComparingTo(new BigDecimal("12.5"));
    }

    @Test
    void saleWithGiftVoucherPaymentMissingGiftVoucher() throws Exception {
        var saleId = sales.createSale(new CreateSaleRequest(SaleType.REGULAR));
        addItem(saleId);

        assertThatThrownBy(() -> addGiftVoucherPaymentMissingGiftVoucher(saleId))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void saleWithGiftVoucherPaymentWithInsufficientBalance() throws Exception {
        var giftVoucher = createGiftVoucher(10.0);
        var saleId = sales.createSale(new CreateSaleRequest(SaleType.REGULAR));
        addItem(saleId);

        assertThatThrownBy(() -> addGiftVoucherPayment(saleId, giftVoucher))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void addItemWhenPaid() throws Exception {
        var saleId = sales.createSale(new CreateSaleRequest(SaleType.REGULAR));
        var sale = "/sales/" + saleId;
        addItem(saleId);
        addPayment(saleId);

        assertThatThrownBy(() -> addItem(sale))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void saleWithInvoice() throws Exception {
        var saleId = sales.createSale(new CreateSaleRequest(SaleType.INVOICE));
        addItem(saleId);
        addPayment(saleId);

        await().untilAsserted(() -> assertThat(invoices.findInvoices(Pageable.unpaged())).hasSize(1));
    }

    private void addGiftVoucherPayment(String saleId, String giftVoucher) throws Exception {
        sales.addPayment(saleId,
                new SaleAddPaymentRequest(new BigDecimal("12.5"), GIFT_VOUCHER, giftVoucher));
    }

    private void addGiftVoucherPaymentMissingGiftVoucher(String saleId) throws Exception {
        sales.addPayment(saleId,
                new SaleAddPaymentRequest(new BigDecimal("12.5"), GIFT_VOUCHER, null));
    }

    private String createGiftVoucher(Double value) throws Exception {
        return giftVouchers.createGiftVoucher(new CreateGiftVoucherRequest(
                "1234",
                new BigDecimal(value)
        ));
    }

    private void addCustomer(String saleId, String customerId) throws Exception {
        sales.addCustomer(saleId, new SaleAddCustomerRequest(customerId));
    }

    private String createCustomer() throws Exception {
        return customers.createCustomer(new CreateCustomerRequest(
                new PersonName("John", "Doe"),
                new Address("Main Street", "1", "1234", "Springfield", "US"),
                "john.doe@test.com"
        ));
    }

    private void addPayment(String id) throws Exception {
        sales.addPayment(id, new SaleAddPaymentRequest(new BigDecimal("12.5"), PaymentMethod.CASH, null));
    }

    private void addItem(String id) throws Exception {
        sales.addItem(id, new SaleAddItemRequest("Llama spray", BigDecimal.ONE, new BigDecimal("12.5")));
    }

}
