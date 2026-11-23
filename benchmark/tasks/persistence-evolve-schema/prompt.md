Products (`com.example.inventory.Product`) are already in production: the database schema migration that was
released is committed in `src/main/resources/db/migration`. Extend products with two new fields:

- `description`: an optional text (may be absent or `null`);
- `stock`: the number of items in stock, a whole number.

Both can be given when a product is created (`POST /products`) and are returned by `GET /products/{id}`. Make sure the
database schema is updated for the new fields.
