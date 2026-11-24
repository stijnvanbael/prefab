---
id: TASK-175.05
title: Make in-memory the default and prove it on the showcase
status: Done
assignee: []
created_date: '2026-11-23 11:00'
labels:
  - test
dependencies:
  - TASK-175.02
  - TASK-175.03
  - TASK-175.04
parent_task_id: TASK-175
priority: high
type: task
ordinal: 156500
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Flip `prefab.test.containers` to default `false`, migrate this repository and document the in-memory backend (TASK-175 rollout, decision-011).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `@IntegrationTest` runs in memory by default
- [x] #2 Tests that call native `@Query` mixins (`examples/kafka`, `pubsub`, `sns-sqs`) use `containers = true`; the reference showcase and existing variants set `prefab.test.containers: true`
- [x] #3 A configuration-only `examples/showcase-inmemory` variant passes with Docker unavailable; gaps (at least Prefab Streams) are declared in its capabilities file with a reason and task
- [x] #4 Wall-clock time of the in-memory variant versus the reference showcase is recorded in TASK-175
- [x] #5 `modules.md`, `feature-guides.md`, `configuration.md`, `annotation-reference.md`, `troubleshooting.md` and `examples/README.md` describe the in-memory backend, its semantics, gaps and the upgrade behaviour change
- [x] #6 A follow-up task exists for Prefab Streams on the in-memory bus
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- **Default flipped.** `TestInfrastructureMode.DEFAULT_CONTAINERS = false`; the two unit tests that asserted `true` now assert in-memory.
- **Examples stay on containers.** `prefab.test.containers: true` in `application-test.yml` of `examples/showcase`, `showcase-mongodb`, `avro`, `kafka`, `mongodb`, `pubsub`, `sns-sqs` and `streams` (created the `prefab:` block in `streams`). `containers = true` on the `MessageIntegrationTest` of `kafka`, `pubsub` and `sns-sqs`. Running `pubsub` and `sns-sqs` with `-Dprefab.test.containers=false` showed that `ChannelSummaryIntegrationTest` also runs the native `UserStatusRepositoryMixin` query, in an event handler (reported as `2 event handler(s) failed during the test`, which is the intended failure mode), so it got `containers = true` too, in all three. The benchmark task `mixin-custom-sql` (native SQL) got `containers = true` in its hidden test. The benchmark module's own unit tests do not use `prefab-test`, so the normal build is unaffected (43 tests pass).
- **New variant `examples/showcase-inmemory`** (`prefab-postgres`, `prefab-kafka`, `prefab-avro` + processor; Streams slices skipped; no `prefab.test.containers`). Registered in the root pom, with a `.run` configuration. Capabilities declared unsupported: `STREAMS` and `KAFKA_STREAMS_BREAKOUT` (TASK-372, new) and `SCHEMA_REGISTRY_API` (TASK-373, new). Coverage matrix in `examples/README.md` regenerated.
- **Showcase test changes for the new capability** (no test weakened): `Capability.SCHEMA_REGISTRY_API` added. `PaymentIntegrationTest.createPayment_registersTheEventSchemaInTheSchemaRegistry` is annotated with it. In `ShipmentIntegrationTest`, the `/subjects` assertion moved from `dispatch_publishesTheEventGeneratedFromTheSchemaFile` into its own test `dispatch_registersTheEventSchemaInTheSchemaRegistry` (same assertion) so the dispatch test keeps running in memory. Both classes inject the registry URL with a default (`${...:}`), because the property only exists when a real registry runs.
- **In-memory bugs found by this first end-to-end run, all fixed in `prefab-test` with a focused test:**
  1. `InMemoryAutoConfigurationFilter.match` threw an NPE: Spring Boot 4.1 passes `null` for candidates an earlier filter already dropped, and `Set.contains(null)` throws. No context could start. Test: `ignoresCandidatesDroppedByAnEarlierFilter`.
  2. Aggregates are identified by a `Reference<T>` while their repositories are keyed by `String`, but the store keyed entries by the `Reference` object, so `findById(String)` and `deleteById(String)` never found anything (every `GET /{id}` was a 404). `AggregateProperties.idOf` now returns `Reference.id()`. Test: fixture `Ticket` with a `Reference` id in `InMemoryRepositoriesTest`.
  3. A `schema.registry.url` configured for the real application (e.g. `http://localhost:8081` in `application.yml`) took precedence over the mock registry, so the first Avro event failed with a connection error. In memory the mock registry is now always used and any configured URL (shared, producer, consumer) is removed. Test: `replacesAConfiguredSchemaRegistryUrlByTheMockRegistry`.
- **Result (no Docker):** `mvn -pl examples/showcase-inmemory test`: 92 tests, 0 failures, 0 errors, 5 skipped (3 by `CapabilityGapsTest`, 2 by `SCHEMA_REGISTRY_API`).
- **Timing (AC #4).** In-memory variant, local, Docker unavailable: 21.9 s Maven time for the module including compilation and annotation processing (sum of test class times 14.1 s). Reference showcase: `Prefab Showcase Example` took 24.1 s in the reactor summary of CI run 45338081735 (`main` at e1cd1601), which includes starting the Testcontainers. The machines differ, so the numbers are not a like-for-like benchmark; the main gain is not needing Docker. The reference module also runs the Streams slices, which the variant skips.
- **Docs:** `feature-guides.md` (new 7.20), `configuration.md`, `annotation-reference.md` (4.10), `troubleshooting.md` (three entries), `modules.md`, `examples/README.md`, `developer-guide.md` index.
- **Follow-ups created:** TASK-372 (Prefab Streams on the in-memory bus), TASK-373 (inspect registered schemas in memory).
- Verification: `mvn install -DskipTests` for the whole reactor, unit and context tests of `core` and `test` (574 and 259 tests, Testcontainers tests excluded), `benchmark` tests, the variant. Container-based example modules cannot run here; they compile.
<!-- SECTION:NOTES:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->
