---
id: TASK-355
title: Scope child aggregate endpoints to their parent path
status: To Do
assignee: []
created_date: '2026-11-22 07:55'
labels:
  - rest
  - bug
  - security
  - agents
dependencies: []
priority: high
type: bug
ordinal: 200000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
For an aggregate with a `@Parent` (for example `Task` with `@Parent Reference<Project> project`), the generated
controller is mapped at `projects/{projectId}/tasks`, but only `create` and `getList` use `{projectId}`. `getById`,
`@Update` and `@Delete` operations take only `{id}`. A task can therefore be read and changed through any project's
URL, for example `GET /projects/<other project>/tasks/<task>`. That is wrong, and a security problem when parents
are owned by different users.

Also:
- `create` responds with `Location: /tasks/{id}`, which is not a route. It should be
  `/projects/{projectId}/tasks/{id}`.
- Naming the parent field `projectId` generates the path variable `{projectIdId}`.

Found by the TASK-343 benchmark: Haiku (benchmark task `rest-parent-child`, run `20261122-065711-claude-haiku-5-5-none`)
noticed the cross-project access and replaced the generated endpoints with a hand-written controller, which the
benchmark guardrails flag as a plain-Spring fallback.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Generated getById, update, delete, download and streaming endpoints of a child aggregate only find the aggregate when it belongs to the parent in the path (404 otherwise)
- [ ] #2 The Location header of a created child points to its nested path
- [ ] #3 The parent path variable name does not duplicate an `Id` suffix
- [ ] #4 Covered by annotation-processor and integration tests
- [ ] #5 The benchmark task rest-parent-child gets a hidden test for cross-parent access
<!-- AC:END -->
