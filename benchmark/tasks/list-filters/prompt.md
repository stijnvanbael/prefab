Products (`com.example.catalog.Product`) can be created and fetched by id. Add `GET /products`, which returns a page
of products (standard Spring Data page JSON, products in the `content` array) and supports two optional filters:

- `category`: only products whose category equals the value, ignoring case (`?category=books` matches `Books`, but
  not `Ebooks`);
- `name`: only products whose name contains the value, ignoring case (`?name=lap` matches `Laptop stand`).

Both filters can be combined; a product must then match both. Without filters all products are returned.
