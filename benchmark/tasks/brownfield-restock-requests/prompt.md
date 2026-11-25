This is our web shop (Spring Boot with Prefab). It is in production: the released database schema migration is
committed in `src/main/resources/db/migration`.

**User story.** As a purchaser, I want to be told automatically when a product is about to sell out, and have the
delivered goods added to the stock, so that we never run out of popular products.

Acceptance criteria:

1. When an order takes the available stock of a product (see `StockLevel`) from 5 or more to below 5, a restock
   request is opened for that product, for 20 items. Further orders while the stock stays below 5 open no new request.
2. Restock requests are a new resource in package `com.example.shop.purchasing`. They are only created automatically:
   there is no endpoint to create one.
3. `GET /restock-requests?product=<product id>` returns a page (requests in the `content` array) of the requests for
   that product, and `GET /restock-requests/{id}` returns one request. Each request has `id`, `product`, `quantity` and
   `status`: `OPEN`, or `FULFILLED` once the goods arrived.
4. When the goods arrive, a purchaser fulfils the request with `POST /restock-requests/{id}/fulfilment`
   (`200 OK`). The request becomes `FULFILLED` and its quantity is added to the available stock of the product.
   Fulfilling a request again changes nothing.
5. Only administrators (role `ADMIN`) may read or fulfil restock requests; other signed-in users get `403 Forbidden`.

Keep the existing tests passing.
