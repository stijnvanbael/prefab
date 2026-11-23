The blog API (`com.example.blog.Article`) is secured with OAuth2 bearer tokens: every request currently needs an
authenticated user. Readers of the public website are anonymous, so:

- `GET /articles` and `GET /articles/{id}` must be accessible without authentication;
- creating articles (`POST /articles`) still requires an authenticated user (`401 Unauthorized` otherwise).
