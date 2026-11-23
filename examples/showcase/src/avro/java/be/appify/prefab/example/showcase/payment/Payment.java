package be.appify.prefab.example.showcase.payment;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.invoicing.Invoice;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.Filter.Operator.EQUAL;

/**
 * A payment of an invoice. Receiving and refunding it publish {@link PaymentEvent Avro events}.
 *
 * @param id      unique reference of the payment
 * @param version optimistic locking version
 * @param invoice the invoice that was paid
 * @param amount  the amount that was received
 * @param method  how the customer paid
 * @param status  where the payment is in its life cycle
 */
@Aggregate
@GetById
@GetList
public record Payment(
        @Id Reference<Payment> id,
        @Version long version,
        @NotNull @Filter(operator = EQUAL) Reference<Invoice> invoice,
        @NotNull BigDecimal amount,
        @NotNull PaymentMethod method,
        @NotNull PaymentStatus status
) {
    private static final EventPublisher<PaymentEvent> EVENTS = EventPublisher.bind(PaymentEvent.class);

    /**
     * Receives a payment and publishes {@link PaymentEvent.Received}.
     *
     * @param invoice the invoice that is paid
     * @param amount  the amount that is received
     * @param method  how the customer pays
     */
    @Create
    public Payment(
            @NotNull Reference<Invoice> invoice,
            @NotNull @Positive BigDecimal amount,
            @NotNull PaymentMethod method
    ) {
        this(Reference.create(), 0L, invoice, amount, method, PaymentStatus.RECEIVED);
        EVENTS.publish(new PaymentEvent.Received(id, invoice, amount, method));
    }

    /**
     * Pays the money back and publishes {@link PaymentEvent.Refunded}.
     *
     * @return the refunded payment
     */
    @Update(path = "/refund", method = "POST")
    public Payment refund() {
        if (status == PaymentStatus.REFUNDED) {
            return this;
        }
        EVENTS.publish(new PaymentEvent.Refunded(id));
        return new Payment(id, version, invoice, amount, method, PaymentStatus.REFUNDED);
    }
}
