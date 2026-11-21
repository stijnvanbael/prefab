Payments (`com.example.payments.Payment`, `POST /payments`) publish a `com.example.payments.PaymentReceived` event on
the Kafka topic `payments`; it carries a reference to the paid order and the amount.

Make orders (`com.example.ordering.Order`) react to these events:

- add the amount to the order's `amountPaid` (a new field, starting at 0);
- when `amountPaid` reaches the order `total` or more, the order `status` becomes `PAID`; until then it stays `OPEN`.

`GET /orders/{id}` shows `amountPaid` and `status`.
