The public web shop shows a product catalogue, but the existing product endpoints (`com.example.catalog.Product`)
expose internal data such as stock levels and `supplierNotes`. Add a dedicated read-only view for the catalogue, as a
record `com.example.catalog.CatalogItem`, served at `GET /catalog-items`:

- it returns a page (standard Spring Data page JSON, items in the `content` array) with, per product, only `name`,
  `price` and `inStock` (`true` when the stock is above 0);
- it supports the same `category` filter as `GET /products`;
- the existing product endpoints stay as they are.
