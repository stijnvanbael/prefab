We are building an online auction site as a Spring Boot application with Prefab. The project is empty; put the code
in package `com.example.auctions`. The API is secured with OAuth2 bearer tokens (already configured); roles are
standard Spring Security roles (authority `ROLE_SELLER`).

**User story.** As a seller, I want to auction an item and sell it to the highest bidder; as a bidder, I want to bid
and see the current highest bid.

Acceptance criteria:

1. Sellers (role `SELLER`) start an auction with `POST /auctions` and `{"item": "Ming vase", "startingPrice": 100}`
   (`201 Created`, `Location` header ending in the id). Other signed-in users get `403 Forbidden`, anonymous requests
   `401 Unauthorized`.
2. `GET /auctions/{id}` (signed in) returns `id`, `item`, `startingPrice`, `status` (`OPEN`), `bidCount`,
   `highestBid` (the amount) and `winningBid` (the id of that bid); both are `null` until there is a valid bid.
3. Any signed-in user bids with `POST /bids` and `{"auction": "<auction id>", "amount": 120}` (`201 Created`).
   `GET /bids/{id}` returns `id`, `auction`, `amount` and `bidder`: the name of the signed-in user who placed it;
   clients cannot choose it.
4. A bid is valid when the auction is open and the amount is at least the starting price and higher than the current
   highest bid. A valid bid becomes the highest bid (`highestBid`, `winningBid`) and increases `bidCount`; other bids
   change nothing on the auction.
5. The seller closes the auction with `POST /auctions/{id}/closing` (role `SELLER`, `200 OK`): status `CLOSED`, later
   bids are not valid. If there was a valid bid, a sale is recorded automatically:
   `GET /sales?auction=<auction id>` (signed in) returns a page (sales in the `content` array) with `id`, `auction`,
   `item`, `bid` (the winning bid) and `amount` (the highest bid). An auction without valid bids has no sale.

The auction and the sales may lag a moment behind bids and closing.
