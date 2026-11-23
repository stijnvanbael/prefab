The product search box needs suggestions while the user types. For products (`com.example.catalog.Product`) add:

- `GET /products/name/autocomplete?query=<text>` returns a JSON array of distinct product names (strings) that start
  with the given text, ignoring case. For example, with products named `Laptop`, `laptop bag`, `Lamp` and `Desk`,
  `query=lap` returns `Laptop` and `laptop bag`.
