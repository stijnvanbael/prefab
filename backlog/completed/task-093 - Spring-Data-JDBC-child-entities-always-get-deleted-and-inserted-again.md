---
id: TASK-093
title: 'Spring Data JDBC: child entities always get deleted and inserted again'
status: Done
assignee:
  - '@agent'
created_date: '2026-03-13 14:51'
updated_date: '2026-03-19 07:28'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 227.81133651733398
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Spring Data JDBC always deletes and inserts all children of an aggregate root on each change to the aggregate root. This has a severe impact on performance and should be avoided.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Multiple solutions are identified and documented
- [x] #2 Solutions are ranked by feasibility, effort, performance and compatibility
- [x] #3 Best solution is implemented in the core module
- [x] #4 PrefabJdbcAggregateTemplate skips child delete+insert when collections are unchanged
- [x] #5 PrefabDataAccessStrategy intercepts and skips unnecessary child DB operations
- [x] #6 PrefabConfiguration registers the new beans
- [x] #7 Existing integration tests still pass
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Analyze root cause and document all solution options
2. Rank solutions by feasibility, effort, performance, compatibility
3. Implement chosen solution: PrefabJdbcAggregateTemplate + PrefabDataAccessStrategy
4. Update PrefabConfiguration to wire new beans
5. Write unit/integration test to verify optimization
6. Update task notes with analysis and implementation details
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Analysis: Spring Data JDBC - Child Entities Always Deleted and Re-inserted

### Root Cause

Spring Data JDBC treats each aggregate as a single unit of change. When `repository.save(aggregateRoot)` is called for an existing entity, the default `JdbcAggregateTemplate` always:
1. Deletes ALL child entity rows for modified parent paths
2. Re-inserts ALL child entity rows from the new state

This happens even when the child collections are completely unchanged — e.g., calling `Sale.addCustomer()` which only modifies the `customer` field triggers deletion and re-insertion of all `Sale.Line` records.

---

## Solution Options (Ranked)

### 1. Pre-Save Load + Thread-Local Skip Mechanism ✅ IMPLEMENTED
**Description:** Override `JdbcAggregateTemplate.save()` to load the current entity before saving, compare child collections using `equals()`, and use a thread-local to skip delete/insert for unchanged collections in a custom `DataAccessStrategy`.
- **Feasibility:** High — fits naturally in the existing Prefab customization pattern
- **Effort:** Medium — 2 new classes, 1 updated configuration
- **Performance:** High improvement — eliminates all unnecessary child DB operations for unchanged collections
- **Compatibility:** Good — no changes to domain model, no annotation processor changes needed
- **Trade-off:** One extra `SELECT` per save for aggregates with collection properties

### 2. Custom DataAccessStrategy Only (No Pre-Load)
**Description:** Wrap `DefaultDataAccessStrategy` and use a `BeforeSaveCallback` with the `MutableAggregateChange` to filter out DELETE DbActions for unchanged paths via reflection on internals.
- **Feasibility:** Low-Medium — requires reflection on Spring Data JDBC internal classes
- **Effort:** Medium
- **Performance:** Same as #1
- **Compatibility:** Low — fragile against Spring Data JDBC version changes

### 3. Add Synthetic @Id to Child Entity Tables
**Description:** Generate auto-increment `@Id` fields for child entity tables. Spring Data JDBC will then use UPDATE for existing children instead of DELETE+INSERT.
- **Feasibility:** High — well-supported by Spring Data JDBC
- **Effort:** Very High — requires annotation processor changes, schema changes, domain model changes
- **Performance:** Best — no unnecessary operations at all
- **Compatibility:** Low — changes the DDD value-object semantics of child entities

### 4. Code Generation: Custom Save Methods per Aggregate
**Description:** Annotation processor generates custom `save()` methods using `JdbcTemplate` directly for diff-based child entity management.
- **Feasibility:** Medium — fits Prefab's code-generation model but complex to generate correctly
- **Effort:** Very High — large annotation processor changes
- **Performance:** Best — most targeted SQL operations possible
- **Compatibility:** High — generated code is self-contained

### 5. BeforeSaveCallback to Replace Entity with Stripped Copy
**Description:** Use `BeforeSaveCallback` to detect unchanged collections and replace the entity with a stripped version (empty lists), preventing child operations.
- **Feasibility:** Low — would CORRUPT data (Spring Data JDBC would DELETE existing children)
- **Effort:** Low
- **Performance:** N/A — not a valid solution
- **Compatibility:** N/A

### 6. Use Spring Data JDBC BeforeSaveCallback + MutableAggregateChange Reflection
**Description:** Access the `actions` list inside `DefaultAggregateChange` via reflection to remove DELETE DbActions for unchanged paths.
- **Feasibility:** Low — relies on internal Spring Data JDBC implementation details
- **Effort:** Medium
- **Performance:** High
- **Compatibility:** Very Low — breaks on any Spring Data JDBC refactoring

---

## Implemented Solution (#1)

### Files Changed
- `core/src/main/java/be/appify/prefab/core/spring/data/jdbc/PrefabDataAccessStrategy.java` — new class extending `DelegatingDataAccessStrategy` that intercepts `delete(rootId, path)` and `insert(instance, type, ...)` calls, skipping them when the child entity type is in the thread-local skip set
- `core/src/main/java/be/appify/prefab/core/spring/data/jdbc/PrefabJdbcAggregateTemplate.java` — new class extending `JdbcAggregateTemplate`, overrides `save()` to load current state, compare child collections, and set the thread-local skip set
- `core/src/main/java/be/appify/prefab/core/spring/PrefabConfiguration.java` — overrides `dataAccessStrategyBean()` and `jdbcAggregateTemplate()` to wire the new implementations

### How It Works
1. On every `save()` for an existing aggregate with collection properties, the current state is loaded from the database
2. Each collection-typed property is compared using `Objects.equals()` (Java records implement structural equality)
3. For unchanged collections, the component type is added to a thread-local skip set
4. `super.save()` proceeds normally — the `PrefabDataAccessStrategy` intercepts and skips DELETE and INSERT calls for types in the skip set
5. The thread-local is cleared in a `finally` block for safety

### Benefits
- `@Version` increment and optimistic locking work correctly (root UPDATE still executes)
- All lifecycle callbacks (`BeforeSaveCallback`, `AfterSaveCallback`) are still fired
- No changes to domain model or annotation processor
- No schema changes required
<!-- SECTION:NOTES:END -->
