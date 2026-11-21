Add a book catalogue to this application.

A book has a title (required, at most 200 characters), an author (required), an ISBN (required) and a publication
year (a whole number). Put the code in package `com.example.catalog` and call the domain type `Book`.

The REST API must offer:

- `POST /books` with JSON body `{"title": ..., "author": ..., "isbn": ..., "publicationYear": ...}` creates a book and
  responds `201 Created` with a `Location` header ending in the new book's id.
- `GET /books/{id}` returns the book as JSON with the fields `id`, `title`, `author`, `isbn` and `publicationYear`,
  or `404 Not Found` when there is no such book.

Books are stored in the PostgreSQL database.
