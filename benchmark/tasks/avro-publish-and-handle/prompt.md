Shops have cash registers (`com.example.retail.CashRegister`) and sales (`com.example.retail.Sale`); a sale is paid
with `POST /sales/{id}/pay`. The company's event platform requires Avro (with the schema registry) for all events.

- When a sale is paid, publish an event `com.example.retail.SalePaid` with the components `sale`
  (`Reference<Sale>`), `cashRegister` (`Reference<CashRegister>`) and `amount` (double) to the Kafka topic `sales`,
  serialized as Avro. Partition by cash register. (`Reference` is `be.appify.prefab.core.service.Reference`.)
- The cash register of the sale handles that event and adds the amount to its `cashInDrawer`.

`GET /cash-registers/{id}` shows `cashInDrawer`.
