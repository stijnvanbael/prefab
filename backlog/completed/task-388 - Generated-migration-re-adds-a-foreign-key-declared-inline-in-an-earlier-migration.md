---
id: TASK-388
title: Generated migration re-adds a foreign key declared inline in an earlier migration
status: Done
assignee: []
created_date: '2026-11-25 06:05'
labels:
  - persistence
  - migrations
dependencies: []
references:
  - TASK-395
priority: medium
type: bug
ordinal: 213000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When `src/main/resources/db/migration/V1__generated.sql` (a committed copy of Prefab's own first migration) creates a
child table with an inline `REFERENCES` clause, e.g.

```sql
CREATE TABLE "customer_order_history_entry" (
  "customer_order_history" VARCHAR (255) NOT NULL REFERENCES "customer_order_history"("id"), ...
```

the next generated migration (after any unrelated model change) contains

```sql
ALTER TABLE customer_order_history_entry
ADD CONSTRAINT customer_order_history_entry_customer_order_history_fk FOREIGN KEY ("customer_order_history") REFERENCES "customer_order_history"("id");
```

The migration parser does not record inline column references, so every later delta adds a second, identical foreign
key. Found with the TASK-377 `shop` base application (`benchmark/apps/shop`), whose committed V1 is the showcase's
generated script.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Replaying a migration with an inline `REFERENCES` clause records the foreign key, so no later migration adds it again
- [x] #2 A processor test covers a model change on top of a committed migration that has an inline foreign key
<!-- AC:END -->

## Implementation Notes

- JSqlParser can return a column-level inline reference such as `REFERENCES "order"("id")` as one column-spec entry. `ForeignKeyReference` now parses the combined clause as well as the separate-token representation, and `Column` records the resulting reference in the replayed state.
- `SqlStatementParserTest.inlineForeignKeyIsRecordedInColumnState` verifies the parsed column model. `DbMigrationWriterTest.inlineForeignKeyInExistingMigrationIsNotReaddedAfterModelChange` compiles a V1 containing an inline child-table reference, adds a model field, and verifies V2 contains the new column without an `ADD CONSTRAINT`.
- The parser and processor regressions passed through IntelliJ's native JPS runner.
