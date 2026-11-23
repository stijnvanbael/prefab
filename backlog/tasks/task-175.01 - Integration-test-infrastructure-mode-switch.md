---
id: TASK-175.01
title: Integration test infrastructure mode switch
status: To Do
assignee: []
created_date: '2026-11-23 11:00'
labels:
  - test
dependencies: []
parent_task_id: TASK-175
priority: high
type: task
ordinal: 156100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Introduce the switch between in-memory and container infrastructure for `@IntegrationTest` (decision-011, TASK-175 design §2). Until TASK-175.05 flips it, `prefab.test.containers` defaults to `true`, so behaviour is unchanged.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `@IntegrationTest` has a `containers` attribute; when true, a `ContextCustomizerFactory` sets `prefab.test.containers=true` with highest precedence and the customizer is part of the context cache key
- [ ] #2 Without the attribute, `prefab.test.containers` from the test configuration decides; container and in-memory test classes in one module get separate cached contexts
- [ ] #3 In in-memory mode an `AutoConfigurationImportFilter` drops store/broker transport auto-configurations (Boot DataSource, JDBC, Flyway, Data JDBC, MongoDB, Data MongoDB, Kafka, GCP Pub/Sub, AWS SNS/SQS; Prefab JDBC, Mongo, Kafka, Pub/Sub and SNS configurations; prefab-test container auto-configurations) and keeps `PrefabRegistryConfiguration`
- [ ] #4 A test fails the build when a filtered auto-configuration class name no longer exists
- [ ] #5 prefab-test's broker test listeners and `MongoDbTestExecutionListener` only act in containers mode
- [ ] #6 In in-memory mode a resource-less `PlatformTransactionManager` supporting `TransactionSynchronization` is registered, so generated `@Transactional` services start without a DataSource
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
