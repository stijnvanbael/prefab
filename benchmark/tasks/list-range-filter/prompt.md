Invoices (`com.example.billing.Invoice`) can be created and fetched by id. Add `GET /invoices`, returning a page of
invoices (standard Spring Data page JSON, invoices in the `content` array) with these optional filters:

- `amountGreaterThanOrEqual` and `amountLessThanOrEqual`: only invoices whose `amount` lies in that range (bounds
  included); either bound can be used on its own;
- `issuedOnGreaterThanOrEqual`: only invoices issued on or after that date (ISO format, e.g. `2026-03-01`).

All given filters must match.
