This is our web shop (Spring Boot with Prefab). Support forwarded this bug report:

> When we close a customer's account (`POST /customers/{id}/deactivation`), their open orders do show up as
> `CANCELLED`, which is right. But the invoices of those orders are still `OPEN`, so the customer keeps getting payment
> reminders, and the stock that the orders had reserved is never released: `reserved` stays up and `available` stays
> down for those products. When the customer cancels an order themselves (`POST /orders/{id}/cancellation`), the
> invoice is voided and the stock is released as expected.

Find the cause and fix it. Deactivating a customer must have the same effect on invoices and stock as cancelling each
of their open orders. Keep the existing tests passing.
