package be.appify.prefab.example.showcase.payment;

/** Where a payment is in its life cycle. */
public enum PaymentStatus {
    /** The money was received. */
    RECEIVED,
    /** The money was paid back. */
    REFUNDED
}
