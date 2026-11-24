Orders (`com.example.ordering.Order`) are created through `POST /orders` and already announce themselves with an
`OrderPlaced` event on the Kafka topic `orders`. Finance wants the running revenue per customer, computed
continuously from those events, not by querying the orders.

Process the `OrderPlaced` events as a stream. For every placed order, publish the new running total of its customer on
the Kafka topic `customer-revenue`, as a Java record `com.example.ordering.CustomerRevenue` with the components
`customer` (String) and `total` (BigDecimal). The records of one customer must be keyed by the customer's name, and
the payload is JSON.

The tests of this project must run without Docker, so do not add a broker or database just for the stream.
