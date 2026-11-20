---
id: TASK-337.05
title: 'Showcase security, OpenAPI and AsyncAPI documentation'
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-20 19:48'
labels:
  - examples
  - security
dependencies:
  - TASK-337.04
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 176000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add prefab-security, prefab-openapi and prefab-async-api to the showcase. Replaces the scope of TASK-046.

Cover: `@Security` on endpoints (e.g. only admins edit the catalog, customers see only their own orders), `@CreatedBy`/`@LastModifiedBy` filled from the principal, generated OpenAPI with Swagger UI, generated AsyncAPI for the order events. Use a test-friendly auth setup; no secrets in config (env vars only, as today with GOOGLE_OAUTH2_*).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Integration tests cover allowed and forbidden access for at least two roles
- [ ] #2 OpenAPI and AsyncAPI documents are served and checked by a test
- [ ] #3 Application runs locally without real OAuth credentials
- [ ] #4 Demonstrates default security, authority-based access and an endpoint with security disabled (scope carried over from TASK-046)
<!-- AC:END -->
