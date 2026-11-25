This is our web shop (Spring Boot with Prefab). It is in production: the released database schema migration is
committed in `src/main/resources/db/migration`, and the catalog holds thousands of products.

**User story.** As a customer, I want to review products I bought and see how other customers rated a product, so
that I can choose better.

Acceptance criteria:

1. Any signed-in user posts a review of a product with `POST /products/{productId}/reviews` and a body like
   `{"customer": "<customer id>", "rating": 4, "comment": "Great crema"}`. The answer is `201 Created` with a
   `Location` header that ends with the id of the review. The comment is optional (at most 1000 characters).
2. A rating is a whole number from 1 to 5. Anything else is rejected with `400 Bad Request`. Anonymous requests get
   `401 Unauthorized`.
3. `GET /products/{productId}/reviews` lists the reviews of that product only, as a page (the review objects with
   `id`, `customer`, `rating` and `comment` are in the `content` array).
4. `GET /products/{id}` also returns `reviewCount`, the number of reviews of the product, and `averageRating`, the
   average of their ratings rounded to one decimal (for ratings 5 and 4: `4.5`). A product without reviews has
   `reviewCount` 0 and no average (`null`). The product summary may lag a moment behind a new review.
5. Products that are already in the database start with 0 reviews after the upgrade.

Keep the existing tests passing.
