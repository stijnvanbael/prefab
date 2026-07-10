---
id: TASK-264
title: Support transient fields on aggregates and value objects (not persisted to DB)
status: To Do
assignee: []
created_date: '2026-07-10 11:32'
labels:
  - feature-request
dependencies: []
priority: medium
---

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A field annotated with a new annotation (e.g. @Transient) is excluded from the generated JPA entity and Flyway migration
- [ ] #2 The transient field is still included in REST request and response DTOs
- [ ] #3 The transient field is still included in event payloads if applicable
- [ ] #4 This supersedes any workaround previously achievable via @CustomType
<!-- AC:END -->
