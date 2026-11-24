---
id: TASK-337
title: Rework examples into one shop showcase with technology variants
status: Done
assignee: []
created_date: '2026-11-20 19:46'
updated_date: '2026-11-24 11:00'
labels:
  - examples
  - docs
dependencies: []
priority: high
type: enhancement
ordinal: 171000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Why
`examples/` holds six independent apps (avro, kafka, mongodb, pubsub, sns-sqs, streams) with five unrelated domains (chat, products/invoices, Avro sales, energy streams, Mongo shapes). `pubsub` and `sns-sqs` are near-identical copies of the chat domain. That makes it hard to see what Prefab offers, and gaps in technology coverage are invisible.

## Target
One example application, `examples/showcase`, in a **shop** domain (catalog, customers, orders, invoices, sales analytics) that shows the most important Prefab features on the reference stack **Postgres + Kafka (JSON)**. Variant modules (`showcase-mongodb`, `showcase-pubsub`, `showcase-sns-sqs`) contain only a pom, configuration and a capabilities file, and prove the *same* source works on another technology.

## Key design constraints (found during analysis)
- The annotation processor decides what to generate from the **compile classpath** (`DbMigrationPlugin` → Spring Data Relational, `MongoIndexPlugin` → `MongoTemplate`, `ControllerUtil` → Spring Security/OpenAPI; `@Event` defaults to `Platform.DERIVED`). A variant therefore cannot depend on a compiled showcase jar: it must **recompile the showcase sources** with its own dependencies.
- Mechanism (decision-007): each variant copies `../showcase/src/main/java` and `../showcase/src/test/java` into `target/showcase/src/{main,test}/java` and compiles them with its own dependencies. Tests are written once and run per variant; shared IntelliJ run configurations in `.run/` run them per variant. Rejected: `build-helper add-source ../showcase/src` (IntelliJ forbids a source root shared by two modules), unpacking classifier source jars (stale in the IDE until the showcase is installed) and running a compiled test-jar (generated-API drift surfaces only as `NoSuchMethodError` at runtime).
- `@IntegrationTest` (prefab-test) already selects Testcontainers from the classpath, so tests need no per-variant changes.
- Features that cannot compile on every stack (Prefab Streams depends on Kafka; Avro is chosen per event in source) live in optional source folders (`src/streams/java`, `src/avro/java`) that only supporting variants include.
- Gaps are explicit: tests declare `@RequiresCapability(...)`, variants declare supported capabilities; unsupported ones are reported as skipped tests with a reason and a backlog task ID, and the coverage matrix in `examples/README.md` is derived from the same files.

Supersedes TASK-043, TASK-044 and TASK-046.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 examples/ contains showcase plus config-only variant modules; the old six example modules are removed
- [x] #2 Every showcase test runs on every variant, or is reported as skipped with the missing capability, reason and backlog task
- [x] #3 examples/README.md shows a technology coverage matrix that a test keeps in sync with the variants' capability files
- [x] #4 Decision recorded as an ADR in backlog/decisions
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
All eleven subtasks are done. `examples/` now holds `showcase`, `showcase-variant-parent` and the variants
`showcase-inmemory`, `-mongodb`, `-pubsub` and `-sns-sqs`. The mechanism is recorded in decision-007 and the coverage
matrix in `examples/README.md` is kept in sync by `CoverageMatrixTest`.
<!-- SECTION:NOTES:END -->
