Articles (`com.example.blog.Article`) have a `title` and a `body`. Add a search endpoint:

- `GET /articles?query=<text>` returns a page of articles (standard Spring Data page JSON, articles in the `content`
  array) where every word of the text occurs in the title or the body, ignoring case. A word may occur in either
  field: `query=spring postgres` finds an article titled "Spring tips" whose body mentions PostgreSQL.
- Without `query`, all articles are returned.

Use the database's full-text search capabilities rather than filtering in Java.
