---
id: TASK-246
title: Auto-enable pg_trgm and generate indexes for @Autocomplete fuzzy search fields
status: In Progress
assignee: []
created_date: '2026-06-01 12:34'
updated_date: '2026-06-01 12:36'
labels:
  - autocomplete
  - migration
  - postgres
  - indexing
dependencies:
  - TASK-244
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When an @Autocomplete field uses fuzzy search matching strategy (MatchStrategy.FUZZY), the database migration generator should:

1. Automatically detect fuzzy search @Autocomplete fields on aggregate roots
2. Emit CREATE EXTENSION IF NOT EXISTS "pg_trgm"; in PostgreSQL migration scripts
3. Generate a GIN or GIST index on the fuzzy search field using the trgm operator
4. Emit appropriate index creation SQL for the scan mode and match strategy combination

This ensures that production PostgreSQL databases have the trigram extension enabled and proper indexes in place for performant fuzzy autocomplete queries without manual DBA intervention.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Annotation processor detects @Autocomplete fields with MatchStrategy.FUZZY
- [ ] #2 DbMigration generator emits CREATE EXTENSION IF NOT EXISTS "pg_trgm"; once per migration file when fuzzy search is used
- [ ] #3 DbMigration generator emits GIN or GIST index creation SQL for each fuzzy-search @Autocomplete field with appropriate trigram operators
- [ ] #4 Index names follow consistent naming convention (e.g., idx_{tableName}_{fieldName}_trgm)
- [ ] #5 Generated migrations are syntactically valid PostgreSQL and execute without errors
- [ ] #6 Test coverage includes scenarios with single fuzzy field, multiple fuzzy fields, and mixed match strategies
<!-- AC:END -->
