---
id: TASK-245
title: Include actuator endpoint by default on Prefab applications
status: Done
assignee: []
created_date: '2026-06-01 12:08'
updated_date: '2026-06-01 12:18'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab should expose at least one Spring Boot Actuator endpoint out of the box in generated applications so health and readiness checks work without extra manual setup.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Generated Prefab applications include Spring Boot Actuator dependency and baseline configuration by default.
- [x] #2 A default actuator endpoint is exposed and reachable in a generated application without additional user configuration.
- [x] #3 Developer guide documentation is updated to explain the default actuator behavior and how to override it.
- [x] #4 Automated tests verify the default actuator endpoint is present in generated output.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented actuator defaults in `prefab-core` so generated applications inherit baseline observability behavior out of the box.

- Added `spring-boot-starter-actuator` to `core/pom.xml` so applications depending on `prefab-core` include Actuator by default.
- Added default baseline management properties in `core/src/main/resources/application.properties`:
  - `management.endpoints.web.exposure.include=health,info`
  - `management.endpoint.health.probes.enabled=true`
- Added integration test `ActuatorDefaultsIntegrationTest` that boots a minimal app and verifies `/actuator/health` and `/actuator` endpoint links are reachable by default.
- Updated developer docs in `backlog/docs/configuration.md` with the new default properties and override guidance.

Verification:
- `mvn -pl core -Dtest=ActuatorDefaultsIntegrationTest test` (pass)
- `mvn -pl core test` (no failures in surefire reports)
<!-- SECTION:NOTES:END -->
