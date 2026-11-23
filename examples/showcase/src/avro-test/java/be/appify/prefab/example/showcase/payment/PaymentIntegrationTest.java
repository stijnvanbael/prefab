package be.appify.prefab.example.showcase.payment;

import be.appify.prefab.example.showcase.payment.infrastructure.http.ReceiptResponse;
import be.appify.prefab.test.IntegrationTest;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Code-first Avro events: published to Kafka with their schema in the schema registry, then read back and decoded by
 * the event handlers of {@link Receipt}.
 */
@IntegrationTest
class PaymentIntegrationTest {

    @Autowired
    PaymentClient payments;
    @Autowired
    ReceiptClient receipts;
    @Value("${spring.kafka.producer.properties.schema.registry.url}")
    String schemaRegistryUrl;

    @Test
    void createPayment_publishesAnAvroEventThatIsConsumedAndDecoded() throws Exception {
        var paymentId = payments.createPayment(invoiceId(), new BigDecimal("42.50"), PaymentMethod.CREDIT_CARD);

        var receipt = receiptOf(paymentId);

        assertThat(receipt.amount()).isEqualByComparingTo("42.50");
        assertThat(receipt.voided()).isFalse();
    }

    @Test
    void createPayment_registersTheEventSchemaInTheSchemaRegistry() throws Exception {
        var paymentId = payments.createPayment(invoiceId(), BigDecimal.TEN, PaymentMethod.CASH);
        receiptOf(paymentId);

        var subjects = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(schemaRegistryUrl + "/subjects")).build(),
                HttpResponse.BodyHandlers.ofString()).body();

        assertThat(subjects).as("schema registry subjects").containsIgnoringCase("showcase.payment");
    }

    @Test
    void refund_voidsTheReceiptThroughAnotherAvroEvent() throws Exception {
        var paymentId = payments.createPayment(invoiceId(), BigDecimal.ONE, PaymentMethod.CASH);
        receiptOf(paymentId);

        payments.refund(paymentId);

        await().atMost(ofSeconds(20)).untilAsserted(() ->
                assertThat(receiptOf(paymentId).voided()).isTrue());
        assertThat(payments.getPaymentById(paymentId).status()).isEqualTo(PaymentStatus.REFUNDED);
    }

    private static String invoiceId() {
        return UUID.randomUUID().toString();
    }

    private ReceiptResponse receiptOf(String paymentId) {
        return await().atMost(ofSeconds(30)).until(
                () -> receipts.findReceipts(Pageable.unpaged(), paymentId).getContent().stream().findFirst(),
                found -> found.isPresent()).orElseThrow();
    }
}
