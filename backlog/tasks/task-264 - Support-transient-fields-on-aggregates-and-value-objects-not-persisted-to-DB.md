---
id: TASK-264
title: Support transient fields on aggregates and value objects (not persisted to DB)
status: In Progress
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

## Analysis
- Current persistence exclusions are split between database migration generation in `annotation-processor` and runtime PostgreSQL mapping in `postgres`.
- `@CustomType` currently skips Flyway and Avro mappings for an entire type, which is too broad for field-level non-persisted state.
- REST request/response generation and Avro event generation already include all fields by default, so the change should stay scoped to persistence-specific code paths.
