The invoicing API (`com.example.billing.Invoice`) is secured with OAuth2 bearer tokens, but every authenticated user
can currently do everything. Restrict it:

- creating an invoice (`POST /invoices`) requires the role `ACCOUNTANT`;
- reading invoices (`GET /invoices/{id}` and `GET /invoices`) is allowed for every authenticated user;
- add `DELETE /invoices/{id}` (responds `204 No Content`), allowed only for the role `ADMIN`.

Unauthenticated requests get `401 Unauthorized`; authenticated users without the required role get `403 Forbidden`.
Roles are standard Spring Security roles (authority `ROLE_ACCOUNTANT`, `ROLE_ADMIN`).
