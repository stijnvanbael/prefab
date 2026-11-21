A colleague started on this feature but the build no longer compiles: orders (`com.example.ordering.Order`) should
add every `com.example.payments.PaymentReceived` event (Kafka topic `payments`) to their `amountPaid`, and become
`PAID` once `amountPaid` reaches the order `total`.

Find out why the build fails, fix it, and make sure the feature works as described. Keep the event and the payment
API as they are.
