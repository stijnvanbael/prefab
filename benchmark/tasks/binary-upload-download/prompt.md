Add document uploads, in package `com.example.documents` with domain type `Document`. A document has a `title` and a
`file`.

- `POST /documents` takes a `multipart/form-data` request with a JSON part `body` holding the other fields
  (`{"title": ...}`) and a file part `file`; it stores the document and responds `201 Created` with a `Location` header
  ending in the document id;
- only PDF (`application/pdf`) and PNG (`image/png`) files of at most 1 MB (1,048,576 bytes) are accepted; other files
  are rejected with `400 Bad Request`;
- `GET /documents/{id}` returns the document metadata as JSON, including its `title`;
- `GET /documents/{id}/file` downloads the file: the original bytes, with the file's content type.
