We are building the loyalty programme of a chain of coffee bars as a Spring Boot application with Prefab. The
project is empty; put the code in package `com.example.loyalty`. The API is secured with OAuth2 bearer tokens (already
configured); roles are standard Spring Security roles (authority `ROLE_CASHIER`).

**User story.** As a customer, I want to collect points with every purchase and spend them on free coffee; as the
owner, I want regular customers to reach the gold tier, and nobody to spend points they do not have.

Acceptance criteria:

1. Any signed-in user opens a loyalty account with `POST /loyalty-accounts` and `{"holder": "Ada"}` (`201 Created`,
   `Location` header ending in the id). `GET /loyalty-accounts/{id}` (signed in) returns `id`, `holder`, `points` (0)
   and `tier` (`BASIC`).
2. Cashiers (role `CASHIER`) register a purchase with `POST /purchases` and `{"account": "<account id>", "amount":
   12.80}` (`201 Created`). The amount must be positive. The account earns one point per whole euro (12.80 gives 12
   points).
3. An account with 1000 points or more is in tier `GOLD`, and stays `GOLD` when points are spent later.
4. Cashiers redeem points with `POST /redemptions` and `{"account": "<account id>", "points": 300}` (`201 Created`).
   `GET /redemptions/{id}` (signed in) returns `id`, `account`, `points` and `status`: `PENDING` at first, then
   `APPROVED` when the account had enough points (they are taken off the account), or `REJECTED` when it did not (the
   account does not change).
5. Purchases and redemptions by users without the role `CASHIER` get `403 Forbidden`; anonymous requests
   `401 Unauthorized`.

Points, tiers and redemption decisions may lag a moment behind the purchase or redemption that causes them.
