Orders (`com.example.ordering.Order`) have a `status` that starts as `OPEN`. Add two actions to the REST API:

- `POST /orders/{id}/ship` (no request body) sets the status to `SHIPPED` and responds `200 OK`;
- `POST /orders/{id}/cancel` (no request body) sets the status to `CANCELLED` and responds `200 OK`.

A shipped order can no longer be cancelled: cancelling it responds `409 Conflict` and leaves the status `SHIPPED`.
`GET /orders/{id}` shows the current `status`.
