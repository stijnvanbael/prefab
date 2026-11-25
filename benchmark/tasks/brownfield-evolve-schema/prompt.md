This is our web shop (Spring Boot with Prefab). It is in production: the released database schema migration is
committed in `src/main/resources/db/migration`, and there are thousands of customers in the database.

We are starting a loyalty programme. Every customer (`com.example.shop.customer.Customer`) gets a loyalty tier:
`BRONZE`, `SILVER` or `GOLD`.

- A customer always has a tier. Customers who register from now on start at `BRONZE`, and every existing customer in
  the database must be `BRONZE` after the upgrade, without any manual step.
- `GET /customers/{id}` returns the tier as `loyaltyTier`.
- Administrators (role `ADMIN`) change the tier with `PUT /customers/{id}/loyalty-tier` and body
  `{"loyaltyTier": "GOLD"}`; it answers `200 OK`. Other users get `403 Forbidden`.

Registering a customer (`POST /customers`) does not change: clients do not send a tier. Keep the existing tests
passing.
