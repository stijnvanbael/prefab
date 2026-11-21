When a customer registers (`POST /customers`), the application publishes a `com.example.crm.CustomerRegistered`
event on the Kafka topic `customers`. Add a loyalty programme that reacts to it, in package `com.example.loyalty`:

- for every registered customer a loyalty account (domain type `LoyaltyAccount`) is created automatically, with
  `customer` (a reference to the customer) and `points` (a whole number, starting at 0);
- loyalty accounts cannot be created through the REST API;
- `GET /loyalty-accounts?customer=<customer id>` returns a page (standard Spring Data page JSON, accounts in the
  `content` array) with the loyalty account of that customer; each account has the fields `id`, `customer` and
  `points`.
