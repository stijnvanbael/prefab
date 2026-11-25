---
id: TASK-381
title: In-memory test mode fails on MongoDB applications
status: Done
assignee: []
created_date: '2026-11-24 07:00'
updated_date: '2026-11-25 06:00'
labels:
  - test
  - mongodb
dependencies:
  - TASK-175.05
priority: medium
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while running `examples/showcase-mongodb` with `-Dprefab.test.containers=false` (TASK-337.08).

`InMemoryAutoConfigurationFilter` excludes `MongoAutoConfiguration` and `DataMongoAutoConfiguration`, but the generated
`MongoIndexConfiguration` (written by `MongoIndexPlugin` into the application's `infrastructure.mongodb` package)
requires a `MongoTemplate` bean. No application context starts in memory mode:

```
Error creating bean with name 'mongoIndexConfiguration' ... No qualifying bean of type
'org.springframework.data.mongodb.core.MongoTemplate' available
```

Decide whether the in-memory backend provides a `MongoTemplate` stand-in, or the generated configuration is skipped
in memory mode (for example `@ConditionalOnBean(MongoTemplate.class)`), and cover it with a test. In-memory is the default
since TASK-175.05, but `showcase-mongodb` keeps `prefab.test.containers: true` until this is fixed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `showcase-mongodb` starts its application context with `prefab.test.containers=false`
- [x] #2 A unit test covers the chosen behaviour of the generated index configuration in memory mode
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Decision: the generated configuration is skipped in memory mode; the in-memory backend does not provide a
`MongoTemplate` stand-in (it has no index concept, and the repositories are `ConcurrentHashMap`s).

- `MongoIndexConfiguration` takes an `ObjectProvider<MongoTemplate>` and creates its indexes inside
  `ifAvailable(...)`. `@ConditionalOnBean(MongoTemplate.class)` was rejected: on a user configuration it is evaluated
  before the Mongo auto-configuration registers `MongoTemplate`, so it would also skip the indexes on real MongoDB.
- Test: `MongoIndexWriterTest.indexesAreOnlyCreatedWhenAMongoTemplateIsAvailable` checks the generated constructor and
  the `ifAvailable` guard; the existing tests still check each generated index statement.
- Starting `showcase-mongodb` in memory exposed two more gaps, fixed in the variant (not in Prefab):
  - the common streams slice needs a `PrefabStreams` bean: added `prefab-streams-engine`, as in `showcase-inmemory`;
  - the Kafka-native breakout topology cannot run on the engine (decision-012): `KafkaOrderTypeBreakoutConfiguration`
    is skipped when `prefab.test.containers=false`.
- `showcase-mongodb` now sets `prefab.test.containers: false`, and shares the `SCHEMA_REGISTRY_API` gap (TASK-375)
  with `showcase-inmemory`. The coverage matrix and test-infrastructure section of `examples/README.md` are updated.
  Real MongoDB coverage in CI now relies on the Mongo module tests, not the showcase.
- `backlog/docs/feature-guides.md` §7.20 notes that indexes are not created in memory mode.
- Not run here: the reference `showcase` tests and the Testcontainers tests (no Docker in this environment).
<!-- SECTION:NOTES:END -->
