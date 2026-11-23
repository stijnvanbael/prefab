`GET /orders/{id}` (aggregate `com.example.ordering.Order`) returns an order with its lines. Clients now also need
two derived, read-only values in that response:

- `total`: the sum over all lines of `quantity * unitPrice`;
- `lineCount`: the number of lines.

They are calculated from the lines, never sent by clients and not stored separately.
