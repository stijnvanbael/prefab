The application has a `Book` aggregate (`com.example.catalog.Book`) that can be created and fetched by id. Add two
operations to its REST API:

- `PUT /books/{id}` with JSON body `{"title": ..., "author": ...}` changes the title and author of a book (the ISBN and
  publication year stay as they are) and responds `200 OK` with the updated book as JSON.
- `DELETE /books/{id}` deletes the book and responds `204 No Content`. Afterwards `GET /books/{id}` responds
  `404 Not Found`.
