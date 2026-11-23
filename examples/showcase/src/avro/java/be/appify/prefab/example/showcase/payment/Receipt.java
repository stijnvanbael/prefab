package be.appify.prefab.example.showcase.payment;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.Filter.Operator.EQUAL;

/**
 * The receipt of a payment. It is created and voided by event handlers, so it only exists once the Avro events of the
 * payment have been written to the broker, read back and decoded with the schema from the schema registry.
 *
 * @param id      unique reference of the receipt
 * @param version optimistic locking version
 * @param payment the payment the receipt proves
 * @param amount  the amount that was received
 * @param voided  whether the payment was refunded
 */
@Aggregate
@GetById
@GetList
public record Receipt(
        @Id Reference<Receipt> id,
        @Version long version,
        @Filter(operator = EQUAL) Reference<Payment> payment,
        BigDecimal amount,
        boolean voided
) {

    /**
     * Issues the receipt of a received payment.
     *
     * @param event the received payment
     * @return a new receipt
     */
    @EventHandler
    public static Receipt onPaymentReceived(PaymentEvent.Received event) {
        return new Receipt(Reference.create(), 0L, event.payment(), event.amount(), false);
    }

    /**
     * Voids the receipt of a refunded payment.
     *
     * @param event the refunded payment
     * @return the voided receipt
     */
    @EventHandler
    @ByReference(property = "payment")
    public Receipt onPaymentRefunded(PaymentEvent.Refunded event) {
        return new Receipt(id, version, payment, amount, true);
    }
}
