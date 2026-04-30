---
id: TASK-150
title: Return 404 when aggregate not found in async update
status: In Progress
assignee: []
created_date: '2026-04-30 11:23'
updated_date: '2026-04-30 11:23'
labels:
  - bug
dependencies: []
priority: P2
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
In an async update (`@AsyncCommit`), the generated controller always returns HTTP 202 Accepted,
even when the target aggregate does not exist. It should return HTTP 404 Not Found when the
service returns an empty `Optional` (i.e. `findById` found no aggregate).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 The generated controller for an async update returns 404 when the aggregate is not found
- [ ] #2 The generated controller for an async update returns 202 when the aggregate is found
- [ ] #3 A test in `AsyncCommitWriterTest` verifies the 404 behaviour
<!-- AC:END -->
