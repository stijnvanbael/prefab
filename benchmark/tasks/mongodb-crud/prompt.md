This application stores its data in MongoDB. Add notes, in package `com.example.notes` with domain type `Note`. A note
has a `title` (required), a `text` and an `archived` flag (`false` for new notes).

- `POST /notes` with `{"title": ..., "text": ...}` creates a note (`201 Created`, `Location` header ending in the id);
- `GET /notes/{id}` returns `{"id": ..., "title": ..., "text": ..., "archived": ...}`;
- `POST /notes/{id}/archive` (no body) archives the note and responds `200 OK`;
- `GET /notes` returns a page of notes (standard Spring Data page JSON, notes in the `content` array) and supports the
  optional filters `title` (title contains the value, ignoring case) and `archived` (`true` or `false`);
- `DELETE /notes/{id}` deletes a note (`204 No Content`).
