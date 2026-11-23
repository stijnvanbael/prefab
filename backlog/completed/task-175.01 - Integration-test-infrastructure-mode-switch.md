---
id: TASK-175.01
title: Integration test infrastructure mode switch
status: Done
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
- [x] #1 `@IntegrationTest` has a `containers` attribute; when true, a `ContextCustomizerFactory` sets `prefab.test.containers=true` with highest precedence and the customizer is part of the context cache key
- [x] #2 Without the attribute, `prefab.test.containers` from the test configuration decides; container and in-memory test classes in one module get separate cached contexts
- [x] #3 In in-memory mode an `AutoConfigurationImportFilter` drops store/broker transport auto-configurations (Boot DataSource, JDBC, Flyway, Data JDBC, MongoDB, Data MongoDB, Kafka, GCP Pub/Sub, AWS SNS/SQS; Prefab JDBC, Mongo, Kafka, Pub/Sub and SNS configurations; prefab-test container auto-configurations) and keeps `PrefabRegistryConfiguration`
- [x] #4 A test fails the build when a filtered auto-configuration class name no longer exists
- [x] #5 prefab-test's broker test listeners and `MongoDbTestExecutionListener` only act in containers mode
- [x] #6 In in-memory mode a resource-less `PlatformTransactionManager` supporting `TransactionSynchronization` is registered, so generated `@Transactional` services start without a DataSource
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- New package `be.appify.prefab.test.inmemory` in prefab-test: `TestInfrastructureMode` (property name and the single `DEFAULT_CONTAINERS = true` constant; TASK-175.05 flips it to `false`), `ContainersModeContextCustomizer(Factory)`, `InMemoryAutoConfigurationFilter`, `InMemoryTransactionManager` and `InMemoryTestAutoConfiguration`.
- The customizer is a component-less record, so all customizers are equal and `@IntegrationTest(containers = true)` classes share one cached context, separate from those without it. The factory uses `TestContextAnnotationUtils.findMergedAnnotation`, so meta-annotations and `@Nested` enclosing classes work.
- The filter list (`InMemoryAutoConfigurationFilter.EXCLUDED`) was taken from the `AutoConfiguration.imports` of the Boot 4.1.2, Spring Cloud GCP 8.3.1 and Spring Cloud AWS 4.2.2 jars. Boot 4 package names are per technology, e.g. `org.springframework.boot.jdbc.autoconfigure.*`, `org.springframework.boot.mongodb.autoconfigure.*`, `org.springframework.boot.kafka.autoconfigure.*`. Health and metrics contributors of the same technologies are filtered too. `PrefabRegistryConfiguration` is kept.
- `InMemoryAutoConfigurationFilterTest` loads every listed class name, so a rename fails the build. This needed test-scoped dependencies on `prefab-postgres` and `prefab-mongodb` in `test/pom.xml`.
- Broker listeners and `MongoDbTestExecutionListener` return early when `TestInfrastructureMode.containersEnabled(context)` is false. `PersistenceTestExecutionListener` is untouched; it has no CrudRepository beans in memory until TASK-175.02.
- `InMemoryTransactionManager` tracks the transaction through a resource bound to the manager, so participation, `REQUIRES_NEW` suspension, rollback-only and `TransactionSynchronization` callbacks work. The auto-configuration runs before `TransactionAutoConfiguration` so `@Transactional` proxying is enabled, and backs off when another `PlatformTransactionManager` exists.
- `@IntegrationTest` is not documented in `annotation-reference.md`, so only `configuration.md` was updated.
- Docker is unavailable here: only unit tests were run (`mvn -pl test test`, 102 tests, all pass). No container-based tests were run.
<!-- SECTION:NOTES:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->
