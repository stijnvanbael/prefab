---
id: TASK-100
title: Add MongoDB support and extract Postgres into separate library
status: Done
assignee: []
created_date: '2026-03-31 05:23'
updated_date: '2026-04-17 06:55'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Currently all persistence support (Spring Data JDBC, PostgreSQL driver, Flyway) lives directly in `prefab-core`. To allow Prefab users to choose their own database backend, the PostgreSQL/JDBC support must be extracted into a dedicated `prefab-postgres` module and a new `prefab-mongodb` module must be added that provides equivalent functionality on top of Spring Data MongoDB.

The guiding principle is Prefab's philosophy of **start high, dive deep when you need to**: users should be able to switch from Postgres to MongoDB (or use both) by changing a single dependency, with no changes to their domain model or service code.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Create a new `prefab-postgres` Maven module that contains all PostgreSQL/JDBC-specific code currently in `prefab-core` (Spring Data JDBC configuration, `PrefabJdbcDialect`, `PrefabMappingJdbcConverter`, `PrefabNamingStrategy`, `PrefabJdbcAggregateTemplate`, `PrefabDataAccessStrategy`, Flyway auto-configuration, etc.)
- [ ] #2 Move the `org.postgresql:postgresql`, `spring-boot-starter-data-jdbc`, `spring-boot-starter-flyway`, and `flyway-database-postgresql` dependencies from `prefab-core/pom.xml` to `prefab-postgres/pom.xml`
- [ ] #3 `prefab-core` retains only database-agnostic abstractions (repository interfaces, domain service contracts, `StorageService`); it must not have a hard dependency on any JDBC or PostgreSQL artifact after the extraction
- [ ] #4 Add `prefab-postgres` as a module in the root `pom.xml` and add it as a dependency in all existing example modules that currently use PostgreSQL (`examples/avro`, `examples/kafka`, `examples/pubsub`, `examples/sns-sqs`)
- [ ] #5 Create a new `prefab-mongodb` Maven module that provides equivalent persistence support using `spring-boot-starter-data-mongodb` and auto-configures a `MongoTemplate`-backed implementation of Prefab's repository abstractions
- [ ] #6 The `prefab-mongodb` module exposes a Spring Boot auto-configuration that wires up MongoDB-backed repositories in the same way `prefab-postgres` wires up JDBC-backed ones, requiring no annotation changes in user code
- [ ] #7 Add `prefab-mongodb` as a module in the root `pom.xml`
- [ ] #8 All existing tests in `prefab-core`, `prefab-postgres`, and example modules continue to pass after the extraction
- [ ] #9 A new integration-test or example module (or a new test profile) demonstrates a MongoDB-backed aggregate being persisted and retrieved via the Prefab repository abstraction
- [ ] #10 README / module documentation is updated to describe the new `prefab-postgres` and `prefab-mongodb` modules and how to choose between them
<!-- AC:END -->
