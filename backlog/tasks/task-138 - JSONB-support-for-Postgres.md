---
id: TASK-138
title: JSONB support for Postgres
status: Done
assignee: []
created_date: '2026-04-24 08:48'
updated_date: '2026-04-29 14:52'
labels:
  - postgres
dependencies: []
priority: high
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow users to annotate fields on aggregate roots or child entities to store them as JSONB documents rather than relational tables and columns. This enables flexible document storage within a relational schema.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A field on an aggregate root or child entity can be annotated with @DbDocument to store its value as a JSONB column
- [ ] #2 Value objects can be annotated for JSONB storage
- [ ] #3 Lists of child entities can be annotated for JSONB storage
- [ ] #4 The JSONB annotation is processed by the annotation processor and generates the appropriate schema and mapping code
- [ ] #5 Indexing on fields inside a JSONB column is supported via the @Indexed annotation
- [ ] #6 Integration tests cover JSONB read, write, and query scenarios
- [ ] #7 Generated Liquibase/Flyway migration includes the JSONB column definition and any declared indexes
<!-- AC:END -->
