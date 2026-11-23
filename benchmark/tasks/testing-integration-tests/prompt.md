The invoice REST API (`com.example.billing.Invoice`) has no tests yet. Write integration tests that run the
application against a real PostgreSQL database (Docker is available) and cover:

- creating an invoice (`POST /invoices`) and reading it back (`GET /invoices/{id}`) with all its fields;
- paying an invoice (`POST /invoices/{id}/pay`): its status becomes `PAID`;
- paying an already paid invoice is refused with `409 Conflict`.

Use Prefab's test support where it helps. Do not change the production code; only add tests under `src/test/java`.
All tests must pass.
