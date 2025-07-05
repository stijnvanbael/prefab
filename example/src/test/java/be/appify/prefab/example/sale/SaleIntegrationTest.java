package be.appify.prefab.example.sale;

import be.appify.prefab.example.IntegrationTest;
import be.appify.prefab.example.sale.infrastructure.persistence.InvoiceCrudRepository;
import be.appify.prefab.example.sale.infrastructure.persistence.SaleCrudRepository;
import be.appify.prefab.test.kafka.TestConsumer;
import be.appify.prefab.test.pubsub.PubSubContainerSupport;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.TimeUnit;

import static be.appify.prefab.test.kafka.asserts.KafkaAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class SaleIntegrationTest implements PubSubContainerSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SaleCrudRepository saleRepository;
    @TestConsumer(topic = "${kafka.topics.sale.name}")
    private Consumer<String, SaleCompleted> saleConsumer;
    @Autowired
    private InvoiceCrudRepository invoiceRepository;

    @BeforeEach
    void setup() {
        saleRepository.deleteAll();
        invoiceRepository.deleteAll();
    }

    @Test
    void simpleSale() throws Exception {
        var sale = startNewSale(SaleType.REGULAR);
        addItem(sale);
        addPayment(sale);

        mockMvc.perform(get(sale))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.payments", hasSize(1)))
                .andExpect(jsonPath("$.returned").value(0.0))
                .andExpect(jsonPath("$.state").value("COMPLETED"));

        assertThat(saleConsumer).hasReceivedMessages(1)
                .within(5, TimeUnit.SECONDS)
                .where(records -> records.hasSizeGreaterThanOrEqualTo(1)
                        .anySatisfy(rec ->
                                assertThat(rec.value()).isInstanceOf(SaleCompleted.class)));
    }

    @Test
    void saleWithCustomer() throws Exception {
        var customer = createCustomer();

        var sale = startNewSale(SaleType.REGULAR);
        addItem(sale);
        addCustomer(sale, customer);
        addPayment(sale);

        mockMvc.perform(get(sale))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer").value(customer));
    }

    @Test
    void saleWithGiftVoucherPayment() throws Exception {
        var giftVoucher = createGiftVoucher(25.0);
        var sale = startNewSale(SaleType.REGULAR);
        addItem(sale);
        addGiftVoucherPayment(sale, giftVoucher);

        mockMvc.perform(get(sale))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments", hasSize(1)))
                .andExpect(jsonPath("$.payments[0].method").value("GIFT_VOUCHER"));

        mockMvc.perform(get("/gift-vouchers/" + giftVoucher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingValue").value(12.5));
    }

    @Test
    void saleWithGiftVoucherPaymentMissingGiftVoucher() throws Exception {
        var sale = startNewSale(SaleType.REGULAR);
        addItem(sale);

        addGiftVoucherPaymentMissingGiftVoucher(sale)
                .andExpect(status().isBadRequest());
    }

    @Test
    void saleWithGiftVoucherPaymentWithInsufficientBalance() throws Exception {
        var giftVoucher = createGiftVoucher(10.0);
        var sale = startNewSale(SaleType.REGULAR);
        addItem(sale);

        addGiftVoucherPayment(sale, giftVoucher)
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItemWhenPaid() throws Exception {
        var sale = startNewSale(SaleType.REGULAR);
        addItem(sale);
        addPayment(sale);

        addItem(sale)
                .andExpect(status().isConflict());
    }

    @Test
    void saleWithInvoice() throws Exception {
        var sale = startNewSale(SaleType.INVOICE);
        addItem(sale);
        addPayment(sale);

        await().untilAsserted(() -> mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1))));
    }

    private ResultActions addGiftVoucherPayment(String sale, String giftVoucher) throws Exception {
        return mockMvc.perform(post(sale + "/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                          {
                            "amount": 12.5,
                            "method": "GIFT_VOUCHER",
                            "giftVoucher": "%s"
                          }
                        """.formatted(giftVoucher))
        );
    }

    private ResultActions addGiftVoucherPaymentMissingGiftVoucher(String sale) throws Exception {
        return mockMvc.perform(post(sale + "/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                          {
                            "amount": 12.5,
                            "method": "GIFT_VOUCHER"
                          }
                        """)
        );
    }

    private String createGiftVoucher(Double value) throws Exception {
        var location = mockMvc.perform(post("/gift-vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                  {
                                    "code": "1234",
                                    "remainingValue": %f
                                  }
                                """.formatted(value)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(LOCATION);

        assert location != null;
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void addCustomer(String sale, String customer) throws Exception {
        mockMvc.perform(post(sale + "/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                  {
                                    "customer": "%s"
                                  }
                                """.formatted(customer)))
                .andExpect(status().isOk());
    }

    private String createCustomer() throws Exception {
        var location = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": {
                                      "firstName": "John",
                                      "lastName": "Doe"
                                    },
                                    "address": {
                                      "street": "Main Street",
                                      "number": "1",
                                      "postalCode": "1234",
                                      "city": "Springfield",
                                      "country": "US"
                                    },
                                    "email": "john.doe@test.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(LOCATION);
        assert location != null;
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void addPayment(String location) throws Exception {
        mockMvc.perform(post(location + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                  {
                                    "amount": 12.5,
                                    "method": "CASH"
                                  }
                                """))
                .andExpect(status().isOk());
    }

    private ResultActions addItem(String location) throws Exception {
        return mockMvc.perform(post(location + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                          {
                            "description": "Llama spray",
                            "quantity": 1,
                            "price": 12.5
                          }
                        """));
    }

    private String startNewSale(SaleType type) throws Exception {
        var location = mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "%s"
                                }
                                """.formatted(type.name())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(LOCATION);
        assert location != null;
        return location;
    }
}
