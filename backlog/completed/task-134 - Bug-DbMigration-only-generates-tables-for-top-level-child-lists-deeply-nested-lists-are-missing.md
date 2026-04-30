---
id: TASK-134
title: >-
  Bug: DbMigration only generates tables for top-level child lists, deeply
  nested lists are missing
status: Done
assignee: []
created_date: '2026-04-20'
updated_date: '2026-04-30 06:04'
labels:
  - bug
  - postgres
  - annotation-processor
dependencies: []
priority: high
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When an aggregate root contains a `List` of child entities, and those children themselves contain further nested
`List` fields, the migration writer only generates a table for the top-level child list. Any lists at deeper
nesting levels are silently omitted from the generated migration script, resulting in missing tables and runtime
errors when the application tries to persist deeply nested structures.

Example:

```java
@Aggregate
@DbMigration
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        List<OrderLine> lines        // ✅ table generated
) {}

public record OrderLine(
        String product,
        List<OrderLineNote> notes    // ❌ table NOT generated
) {}

public record OrderLineNote(
        String text                  // missing from migration
) {}
```

The migration writer must recursively traverse child entity types and generate a table for every nested list,
regardless of depth.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The migration writer recursively visits all nested child entity types and generates a table for every `List` field at any depth
- [ ] #2 The foreign key column in each nested child table correctly references the parent table
- [ ] #3 An aggregate with only top-level child lists continues to produce a correct migration (no regression)
- [ ] #4 A test source file with at least two levels of nested child lists is added
- [ ] #5 Tests in `DbMigrationWriterTest` assert that all nested child tables are present in the generated migration
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Locate the migration writer logic that iterates over an aggregate's fields to emit child tables
2. Replace the single-level iteration with a recursive traversal that processes each child record type and its own `List` fields
3. Ensure the foreign key column name in each nested table correctly references its direct parent
4. Add a test source file (e.g. `dbmigration/nestedlists/source/Order.java`) with at least two levels of nesting
5. Add tests to `DbMigrationWriterTest` that verify all nested child tables appear in the generated SQL
<!-- SECTION:PLAN:END -->
