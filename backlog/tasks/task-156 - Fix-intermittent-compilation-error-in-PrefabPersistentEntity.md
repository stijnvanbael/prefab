---
id: TASK-156
title: Fix intermittent compilation error in PrefabPersistentEntity (Spring Data Relational version mismatch)
status: To Do
assignee: []
created_date: '2026-05-03 11:00'
updated_date: '2026-05-03 11:00'
labels:
  - bug
  - compilation
  - postgres
dependencies: []
priority: high
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
CI builds intermittently fail to compile `prefab-postgres` with:

```
PrefabPersistentEntity.java:[43,8] be.appify.prefab.postgres.spring.data.jdbc.PrefabPersistentEntity
is not abstract and does not override abstract method getIdColumn() in
org.springframework.data.relational.core.mapping.RelationalPersistentEntity
```

The error appears on retried CI runs (run_attempt > 1) when Maven's incremental compiler uses a
stale `.class` file for `PrefabPersistentEntity` that was compiled against a different version of
`spring-data-relational` (where `getIdColumn()` was a default method rather than abstract). The
stale class is then validated against the current Spring Data Relational jar where `getIdColumn()`
is abstract, but the stale class doesn't provide an explicit implementation.

This affects both `main` and feature branches. A temporary workaround
(`<useIncrementalCompilation>false</useIncrementalCompilation>` in `postgres/pom.xml`) was added
as part of task 147, but the underlying class hierarchy design should be reviewed — consider
extending `BasicRelationalPersistentEntity` to inherit `getIdColumn()` instead of implementing
it explicitly, or restructure the class to be immune to stale-class issues.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The `prefab-postgres` module compiles successfully on both fresh builds and retried CI runs
- [ ] #2 No intermittent `getIdColumn()` compilation error in any CI run
- [ ] #3 The root cause (stale compiled class OR Spring Data version mismatch) is addressed structurally
<!-- AC:END -->
