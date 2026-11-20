---
id: TASK-337.01
title: Showcase module skeleton with source-sharing build and ADR
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-20 19:47'
labels:
  - examples
dependencies: []
parent_task_id: TASK-337
priority: high
type: task
ordinal: 172000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create `examples/showcase` (Postgres + Kafka, JSON) with a minimal shop aggregate and one integration test, and prove the variant build mechanism end to end with a throw-away or first real variant.

Build mechanism (see TASK-337): `showcase` attaches `sources` and `test-sources` jars (maven-source-plugin `jar-no-fork` + `test-jar-no-fork`). A variant depends on those classifiers only (never on the main jar), unpacks them with maven-dependency-plugin into `target/generated-sources/showcase` and `target/generated-test-sources/showcase`, adds them with build-helper-maven-plugin, and runs prefab-annotation-processor on them. Put the shared variant build config in an `examples/showcase-variant-parent` pom so a variant pom only lists its dependencies.

Record the decision (single showcase, recompile per variant, capability-based gaps, rejected alternatives) as an ADR in `backlog/decisions/`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 examples/showcase builds and its integration test passes on Postgres + Kafka
- [ ] #2 A variant module containing only pom.xml and resources compiles the showcase sources and runs the showcase tests
- [ ] #3 Variant build config is shared via a parent pom; a variant pom lists only its technology dependencies
- [ ] #4 IntelliJ imports showcase and variants without shared-content-root warnings
- [ ] #5 ADR added to backlog/decisions
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. `examples/showcase` (artifact `showcase-example`): Postgres + Kafka (JSON), `ShowcaseApplication`, minimal `catalog.Product` aggregate (`@Create`, `@GetById`) and `ProductIntegrationTest` using the generated `ProductClient`. Attaches `sources` + `test-sources` jars (maven-source-plugin, forced on despite `maven.source.skip` in the development profile; resources excluded so each variant owns its configuration).
2. `examples/showcase-variant-parent` (pom): depends on the two source classifiers only (provided scope, all transitives excluded so the reference stack never leaks into a variant), unpacks them into `target/generated-sources/showcase` and `target/generated-test-sources/showcase`, adds those plus `target/prefab-test-sources` as source roots and runs the Prefab annotation processor. Also carries the technology-neutral dependencies (prefab-core, annotation processor, prefab-test).
3. First real variant `examples/showcase-mongodb` (MongoDB + Kafka): pom with only prefab-mongodb + prefab-kafka, plus application.yml / application-test.yml. Proves the processor regenerates persistence code for a different classpath. TASK-337.08 completes it.
4. ADR decision-007 in `backlog/decisions/`.
5. Register modules in the root pom (showcase before variant parent before variants).
<!-- SECTION:PLAN:END -->

