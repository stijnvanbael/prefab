---
id: TASK-388
title: Generated migration re-adds a foreign key declared inline in an earlier migration
status: To Do
assignee: []
created_date: '2026-11-25 06:05'
labels:
  - persistence
  - migrations
dependencies: []
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
- [ ] #1 Replaying a migration with an inline `REFERENCES` clause records the foreign key, so no later migration adds it again
- [ ] #2 A processor test covers a model change on top of a committed migration that has an inline foreign key
<!-- AC:END -->
