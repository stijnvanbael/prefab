This is our web shop (Spring Boot with Prefab). It is in production: the released database schema migration is
committed in `src/main/resources/db/migration`, and the products table holds real data.

Marketing wants the product field `brand` to be called `manufacturer` everywhere in the product API
(`com.example.shop.catalog.Product`):

- `POST /products` takes `manufacturer` instead of `brand`;
- `GET /products/{id}` and `GET /products` return `manufacturer`;
- `GET /products?manufacturer=<name>` filters products by manufacturer.

Our mobile app is released less often and still reads `brand`, so for now product responses must contain both
`manufacturer` and `brand`, with the same value.

The existing values must survive the database migration: rename the stored column, do not drop it and add a new one.
Keep the existing tests passing.
