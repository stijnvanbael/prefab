Finance wants the revenue per order status. Add this method to the repository that is used for orders
(`com.example.ordering.application.OrderRepository`):

- `BigDecimal totalRevenue(String status)`: the sum of `total` over all orders with that status (e.g. `"SHIPPED"`),
  or `0` when there are none.

The sum must be calculated by the database in a single query, not by loading the orders into memory. Do not create
a second repository for orders.
