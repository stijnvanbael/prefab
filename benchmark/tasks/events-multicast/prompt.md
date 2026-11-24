Customers can watch the price of a product: a price watch (`com.example.watch.PriceWatch`) has an `email`, a
`product` (reference to `com.example.catalog.Product`) and a `currentPrice`. When a product's price changes
(`PUT /products/{id}/price`), the application publishes `com.example.catalog.PriceChanged` on the Kafka topic
`prices`.

Keep every price watch up to date: when `PriceChanged` arrives, set `currentPrice` of **all** watches of that product
to the new price. Watches of other products stay unchanged. If a product has no price watches, handling its price
change should succeed without retrying or dead-lettering the event. `GET /price-watches/{id}` shows the `currentPrice`.
