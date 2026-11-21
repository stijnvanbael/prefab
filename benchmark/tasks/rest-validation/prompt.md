`POST /customers` (aggregate `com.example.crm.Customer`) currently accepts any input. Add validation so that invalid
requests are rejected with `400 Bad Request` and nothing is stored:

- `name` is required, must not be blank and is at most 100 characters long;
- `email` is required and must be a valid e-mail address.

Valid requests keep working as before (`201 Created`).
