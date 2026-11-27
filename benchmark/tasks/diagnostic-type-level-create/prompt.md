A colleague started the book catalogue (`com.example.catalog.Book`) but the build no longer compiles.

Find out why the build fails, fix it, and make sure `POST /books` with JSON body `{"title": ..., "author": ...}`
creates a book and responds `201 Created` with a `Location` header, and `GET /books/{id}` returns it as JSON with
the fields `id`, `title` and `author`. Keep the fields of `Book` as they are.
