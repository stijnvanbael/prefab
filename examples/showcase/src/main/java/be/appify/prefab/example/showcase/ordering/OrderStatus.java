package be.appify.prefab.example.showcase.ordering;

/** Where an order is in its life cycle. */
public enum OrderStatus {
    /** The order was placed and can still be cancelled. */
    PLACED,
    /** The order was cancelled, by the customer or because the customer closed the account. */
    CANCELLED
}
