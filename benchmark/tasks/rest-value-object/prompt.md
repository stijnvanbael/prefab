Add customers with a postal address, in package `com.example.crm`, with domain type `Customer`.

A customer has a `name` and an `address`. An address is a value object with `street`, `city`, `postalCode` and
`country`, all required strings.

- `POST /customers` with body `{"name": ..., "address": {"street": ..., "city": ..., "postalCode": ..., "country": ...}}`
  creates a customer (`201 Created` with a `Location` header ending in the id).
- `GET /customers/{id}` returns `{"id": ..., "name": ..., "address": {"street": ..., "city": ..., "postalCode": ..., "country": ...}}`.
- `PUT /customers/{id}/address` with body `{"address": {...}}` replaces the address and responds `200 OK`.
