A reporting job needs two extra queries on orders (`com.example.ordering.Order`). Add them to the repository that is
used for orders, `com.example.ordering.application.OrderRepository`, so other beans can call them:

- `List<Order> findByCustomerEmail(String customerEmail)`: all orders of a customer;
- `long countByStatus(OrderStatus status)`: the number of orders in a status.

Do not create a second repository for orders.
