---
id: TASK-381
title: In-memory test mode fails on MongoDB applications
status: To Do
assignee: []
created_date: '2026-11-24 07:00'
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
- [ ] #1 `showcase-mongodb` starts its application context with `prefab.test.containers=false`
- [ ] #2 A unit test covers the chosen behaviour of the generated index configuration in memory mode
<!-- AC:END -->
