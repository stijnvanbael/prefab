---
id: TASK-337.02
title: Showcase capability mechanism and technology coverage matrix
status: Done
assignee:
  - '@claude'
created_date: '2026-11-20 19:47'
labels:
  - examples
  - testing
dependencies:
  - TASK-337.01
parent_task_id: TASK-337
priority: high
type: task
ordinal: 173000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Make technology gaps explicit in the showcase test runs and docs.

- A `Capability` enum in the showcase test sources (e.g. FULL_TEXT_SEARCH, RANGE_FILTER, POLYMORPHIC_AGGREGATES, BINARY_DOWNLOAD, ORDERED_DELIVERY, STREAMS, AVRO, SECURITY, OPENAPI, ASYNCAPI; extend as features land).
- `@RequiresCapability(...)` on tests, evaluated by a JUnit 5 `ExecutionCondition` that reads `showcase-capabilities.yml` from the variant's test resources. Each unsupported entry carries a reason and a backlog task ID; the skip message reads like `showcase-pubsub: STREAMS not supported — no Pub/Sub backend for Prefab Streams (TASK-xxx)`.
- Capabilities whose code lives in optional source folders (streams, avro) cannot have their tests compiled on unsupported variants; a `CapabilityGapsTest` emits one dynamic test per unsupported capability that is reported as skipped with its reason, so gaps show up in every build.
- `examples/README.md` contains a coverage matrix (variant × capability, with links to gap tasks). A test fails when the matrix and the capability files disagree.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Tests annotated with @RequiresCapability are skipped with variant, capability, reason and task ID when the variant lacks the capability
- [x] #2 Unsupported capabilities without compiled tests still appear as skipped tests in the Surefire report
- [x] #3 examples/README.md coverage matrix is verified against the capability files by a test
- [x] #4 A variant declaring an unknown capability or a gap without a task ID fails the build
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Package `be.appify.prefab.example.showcase.capability` in `examples/showcase/src/test/java` (copied to every variant by the variant parent): `Capability`, `CapabilityGap`, `VariantCapabilities` (parser and validator of `showcase-capabilities.yml`), `@RequiresCapability` + `CapabilityCondition` (JUnit `ExecutionCondition`), `CapabilityGapsTest` (one skipped dynamic test per gap).
2. `showcase-capabilities.yml` in `src/test/resources` of `showcase` and `showcase-mongodb` (no gaps known yet). The file is mandatory for every variant.
3. `examples/README.md` with the coverage matrix between markers. `CoverageMatrix` renders the table from the capability files of all `examples/showcase*` modules (task links are resolved against `backlog/`, so a gap task that does not exist fails too); `CoverageMatrixTest` compares it with the README and prints the expected table on a mismatch.
4. Unit tests for the parser and the condition.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
All code is in `examples/showcase/src/test/java/be/appify/prefab/example/showcase/capability/` and reaches every variant through the existing source copy.

- **`showcase-capabilities.yml`** (`src/test/resources` of every variant, mandatory): `variant: <module>` plus an optional `unsupported:` map of `CAPABILITY: {reason, task}`. Everything not listed is supported. `VariantCapabilities` rejects unknown capabilities, missing/blank reasons, tasks that are not `TASK-<n>[.<n>]`, unexpected keys and duplicate keys. `showcase` and `showcase-mongodb` have no gaps (nothing is known to be missing on MongoDB + Kafka).
- **`@RequiresCapability` + `CapabilityCondition`**: skip message is `<variant>: <CAPABILITY> not supported — <reason> (<task>)`; several gaps are joined with `; `.
- **`CapabilityGapsTest`**: one `@TestFactory` dynamic test per gap that calls `Assumptions.abort(message)`, so Surefire reports it as skipped with the message. It also loads the capabilities file in every variant, so a missing or invalid file fails the build even when no test is annotated.
- **Coverage matrix**: the table in `examples/README.md` between `coverage-matrix:begin/end` markers. `CoverageMatrix` renders it from the capability files of all `examples/showcase*` modules (reference `showcase` first, `showcase-variant-parent` excluded) and links each gap to its task file in `backlog/{tasks,completed,archive}`; a gap task that does not exist fails the test. `CoverageMatrixTest` compares and prints the expected table on a mismatch. It also checks that `variant:` equals the module directory. It runs in every variant (cheap) and finds `examples/` by walking up from the working directory.
- Capabilities not covered by any test yet (RANGE_FILTER, STREAMS, AVRO, ...) are in the enum already so the matrix is complete; later tasks add `@RequiresCapability` where they land.

**Verification** (no Docker in this environment, so `ProductIntegrationTest` was not run): the 14 capability tests pass on `showcase` and `showcase-mongodb`. Experiment with a temporary STREAMS gap on `showcase-mongodb`: a `@RequiresCapability(STREAMS)` test and the dynamic gap test were both reported `<skipped message="showcase-mongodb: STREAMS not supported — … (TASK-341)">`, and `CoverageMatrixTest` failed until the README was updated, printing the expected row with the TASK-341 link.

**For the next tasks**: TASK-337.09/.10 must add their `showcase-capabilities.yml` (STREAMS → TASK-341/TASK-342, ORDERED_DELIVERY on SNS/SQS → TASK-340, AVRO → TASK-275 where applicable) and a column to the README matrix (paste the table `CoverageMatrixTest` prints).
<!-- SECTION:NOTES:END -->
