Other services need to know when an order is placed. Whenever an order (`com.example.ordering.Order`) is created
through `POST /orders`, publish an event to the Kafka topic `orders`:

- the event is a Java record `com.example.ordering.OrderPlaced` with the components `order` (a
  `be.appify.prefab.core.service.Reference<Order>` to the new order), `customer` (String) and `total` (BigDecimal);
- events of the same order must always land on the same partition (partition by the order);
- the payload is JSON.
