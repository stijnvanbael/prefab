package be.appify.prefab.example.showcase.invoicing;

/** Where an invoice is in its life cycle. */
public enum InvoiceStatus {
    /** The invoice waits for payment. */
    OPEN,
    /** The invoice was paid. */
    PAID,
    /** The order was cancelled, so nothing is due. */
    VOID
}
