We are building a ticket shop for concerts as a Spring Boot application with Prefab. The project is empty; put the
code in package `com.example.tickets`. The API is secured with OAuth2 bearer tokens (already configured); roles are
standard Spring Security roles (authority `ROLE_ORGANIZER`).

**User story.** As a concert organizer, I want to put concerts on sale and see how many tickets are left; as a fan, I
want to find concerts in my city and buy or cancel tickets.

Acceptance criteria:

1. Organizers (role `ORGANIZER`) add a concert with `POST /concerts` and
   `{"name": "Night Owls", "venue": "Arena", "city": "Ghent", "capacity": 500}` (`201 Created`, `Location` header
   ending in the id). Other signed-in users get `403 Forbidden`, anonymous requests `401 Unauthorized`.
2. Anyone, also without signing in, can read `GET /concerts/{id}` and `GET /concerts?city=<city>` (a page, concerts in
   the `content` array, only concerts in that city). A concert has `id`, `name`, `venue`, `city`, `capacity`,
   `ticketsSold`, `remaining` (capacity minus tickets sold) and `soldOut` (`true` when nothing remains).
3. A signed-in user buys tickets with `POST /ticket-orders` and `{"concert": "<concert id>", "quantity": 2}`
   (`201 Created`); `quantity` is from 1 to 10, anything else is `400 Bad Request`. Anonymous requests get
   `401 Unauthorized`. `GET /ticket-orders/{id}` (signed in) returns `id`, `concert`, `quantity` and `status`
   (`PLACED`).
4. The concert counts the sold tickets: `ticketsSold` goes up by the quantity of every order.
5. `POST /ticket-orders/{id}/cancellation` (signed in, `200 OK`) cancels an order: its status becomes `CANCELLED` and
   its tickets go back on sale. Cancelling twice changes nothing.

The counters of a concert may lag a moment behind orders and cancellations.
