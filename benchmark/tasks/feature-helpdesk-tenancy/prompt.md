We are building a helpdesk that several companies (tenants) share, as a Spring Boot application with Prefab. The
project is empty; put the code in package `com.example.helpdesk`. Every request carries the tenant in the HTTP header
`X-Tenant-Id`; the API gateway guarantees it is present and trustworthy.

**User story.** As a support agent, I want to register customer tickets, discuss them in comments and resolve them,
and I must never see the tickets of another company.

Acceptance criteria:

1. `POST /tickets` with `{"subject": "Printer jams", "description": "Every second page"}` opens a ticket
   (`201 Created`, `Location` header ending in the id). `GET /tickets/{id}` returns `id`, `subject`, `description`,
   `status` (`OPEN`) and `commentCount` (0). The subject is required (`400 Bad Request` otherwise).
2. `POST /tickets/{ticketId}/comments` with `{"author": "Sam", "text": "Looking into it"}` adds a comment to a ticket
   (`201 Created`). `GET /tickets/{ticketId}/comments` lists the comments of that ticket as a page (comments in the
   `content` array, with `id`, `author` and `text`).
3. Every comment adds one to the `commentCount` of its ticket, and the first comment on an `OPEN` ticket moves it to
   `IN_PROGRESS`. (The ticket may lag a moment behind its comments.)
4. `POST /tickets/{id}/resolution` resolves a ticket (`200 OK`, status `RESOLVED`).
5. `GET /tickets?status=<status>` lists the tickets with that status, as a page.
6. Tenants are isolated: tickets and comments belong to the tenant of the request that created them; clients never
   send the tenant in the body. Another tenant's ticket is `404 Not Found` and never appears in its lists.
