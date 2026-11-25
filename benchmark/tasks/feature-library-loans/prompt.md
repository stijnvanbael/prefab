We are building the lending system of a public library as a Spring Boot application with Prefab. The project is
empty; put the code in package `com.example.library`.

**User story.** As a librarian, I want to lend books to members and take them back, and always see how many copies of
a book are on the shelf and how many books a member has at home.

Acceptance criteria:

1. `POST /books` with `{"title": "Dune", "author": "Frank Herbert", "copies": 3}` adds a book (`201 Created`,
   `Location` header ending in the id). `GET /books/{id}` returns `id`, `title`, `author`, `copies` and
   `availableCopies`, which starts equal to `copies`.
2. `POST /members` with `{"name": "Ada", "email": "ada@example.com"}` registers a member. `GET /members/{id}` returns
   `id`, `name`, `email` and `activeLoans`, which starts at 0.
3. `POST /loans` with `{"book": "<book id>", "member": "<member id>"}` lends a book (`201 Created`).
   `GET /loans/{id}` returns `id`, `book`, `member` and `status` (`ACTIVE`). The book then has one available copy less
   and the member one active loan more.
4. `POST /loans/{id}/return` takes the book back (`200 OK`): the loan becomes `RETURNED`, the book gets the copy back
   and the member has one active loan less. Returning a loan that was already returned changes nothing.
5. `GET /loans?member=<member id>` lists the loans of one member, as a page (loans in the `content` array).
6. Titles, authors, names and emails are required; `copies` is at least 1. Invalid input gets `400 Bad Request`.

Counters on books and members may lag a moment behind loans and returns.
