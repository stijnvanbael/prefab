Products (`com.example.catalog.Product`) can be created and fetched by id. Add a list endpoint:

- `GET /products` returns a page of products in the standard Spring Data page JSON format (the products are in the
  `content` array, and the page metadata includes `totalElements`);
- clients page with the `page` (zero-based) and `size` query parameters;
- when the client does not ask for a sort order, products are sorted by `name`, A to Z;
- clients can sort explicitly, e.g. `GET /products?sort=price,desc`.
