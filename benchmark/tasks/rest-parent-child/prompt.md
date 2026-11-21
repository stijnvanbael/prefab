The application manages projects (`com.example.planning.Project`). Add tasks that belong to a project, as their own
domain type `Task` in the same package. A task has a `title` (required) and a `done` flag that is `false` when the
task is created.

Tasks are a sub-resource of their project:

- `POST /projects/{projectId}/tasks` with body `{"title": ...}` creates a task in that project (`201 Created`, with a
  `Location` header ending in the task id);
- `GET /projects/{projectId}/tasks/{id}` returns `{"id": ..., "title": ..., "done": ...}`;
- `GET /projects/{projectId}/tasks` returns a page of the tasks of that project only: a JSON object whose `content`
  array holds the tasks (the standard Spring Data page format);
- `POST /projects/{projectId}/tasks/{id}/complete` marks the task as done and responds `200 OK`.
