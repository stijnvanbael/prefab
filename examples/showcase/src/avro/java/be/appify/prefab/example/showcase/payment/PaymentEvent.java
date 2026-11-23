package be.appify.prefab.example.showcase.payment;

import be.appify.prefab.core.annotations.Decimal;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.invoicing.Invoice;
import java.math.BigDecimal;

/**
 * Everything that happens to a payment, serialized as Avro.
 *
 * <p>This is the code-first way to use Avro: the records are the source of truth and Prefab generates the Avro schema,
 * the converters and the registration in the schema registry from them. Only {@code serialization} differs from the
 * JSON events of the other showcase aggregates; publishing and handling look the same.</p>
 */
@Event(topic = "showcase.payment", serialization = Event.Serialization.AVRO)
public sealed interface PaymentEvent permits PaymentEvent.Received, PaymentEvent.Refunded {

    /**
     * The payment the event is about.
     *
     * @return the payment, which is also the partitioning key
     */
    @PartitioningKey
    Reference<Payment> payment();

    /**
     * A customer paid an invoice.
     *
     * @param payment the new payment
     * @param invoice the invoice that was paid
     * @param amount  the amount that was received; Avro stores it as a decimal with two digits after the point
     * @param method  how the customer paid
     */
    record Received(
            Reference<Payment> payment,
            Reference<Invoice> invoice,
            @Decimal(precision = 12, scale = 2) BigDecimal amount,
            PaymentMethod method
    ) implements PaymentEvent {
    }

    /**
     * A payment was paid back to the customer.
     *
     * @param payment the refunded payment
     */
    record Refunded(Reference<Payment> payment) implements PaymentEvent {
    }
}
