---
id: TASK-132
title: Autoconfigure datasource and driver class name for PostgreSQL
status: Done
assignee: []
created_date: '2026-04-20'
updated_date: '2026-04-22 13:38'
labels:
  - postgres
dependencies: []
priority: medium
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When using `prefab-postgres`, developers must manually configure `spring.datasource.driver-class-name` and often
also the datasource URL in their `application.yml`. Since the PostgreSQL driver is already a transitive dependency
of `prefab-postgres`, Prefab can provide sensible Spring Boot auto-configuration defaults so that a minimal setup
just works out of the box.

Provide auto-configuration in the `prefab-postgres` module that sets `spring.datasource.driver-class-name` to
`org.postgresql.Driver` as a default. Additional sensible defaults (e.g. connection pool settings) may also be
considered.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Adding `prefab-postgres` to the classpath automatically sets `spring.datasource.driver-class-name` to `org.postgresql.Driver` without any manual configuration
- [x] #2 A developer-supplied `spring.datasource.driver-class-name` in `application.yml` takes precedence over the Prefab default
- [x] #3 The auto-configuration does not interfere with other datasource properties (URL, username, password)
- [x] #4 Tests verify that the default driver class name is applied and that it can be overridden
- [x] #5 Documentation is updated to describe the default auto-configuration and how to override it
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add a `additional-spring-configuration-metadata.json` (or `spring-configuration-metadata.json`) to `prefab-postgres` that declares `spring.datasource.driver-class-name=org.postgresql.Driver` as a default
2. Alternatively, create a `PostgresAutoConfiguration` class annotated with `@AutoConfiguration` and `@ConditionalOnMissingBean(DataSource.class)` that sets the driver property via `SpringApplication.setDefaultProperties`
3. Register the auto-configuration in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
4. Write an integration test that verifies the driver class name is set without any explicit configuration
5. Write a test that verifies an explicit property in `application.yml` overrides the default
6. Update `readme.md` to document the auto-configured defaults
<!-- SECTION:PLAN:END -->
