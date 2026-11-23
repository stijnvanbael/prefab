package be.appify.prefab.example.showcase.payment;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import java.util.List;

/** Query that {@link Receipt} uses to find the receipts a payment event is delivered to. */
@RepositoryMixin(Receipt.class)
public interface ReceiptRepositoryMixin {

    /**
     * Finds the receipts of a payment.
     *
     * @param payment the payment
     * @return the receipts of the payment
     */
    List<Receipt> findByPayment(Reference<Payment> payment);
}
