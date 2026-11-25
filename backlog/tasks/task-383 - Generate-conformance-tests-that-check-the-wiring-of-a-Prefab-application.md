---
id: TASK-383
title: Generate conformance tests that check the wiring of a Prefab application
status: In Progress
assignee: []
created_date: '2026-11-24 10:00'
labels:
  - testing
  - agents
  - productivity
dependencies:
  - TASK-175
priority: high
type: feature
ordinal: 204000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Haiku now passes 29-31 of 32 benchmark tasks (TASK-343). The failures left over that are not Prefab bugs share one
shape: the change looks right, the agent's own tests pass, and the app is broken on the side the agent did not test.

- `security-public-endpoints`: an extra `SecurityFilterChain` silently replaced Prefab's default chain; anonymous GETs
  worked, every authenticated write returned 401. Haiku and Sonnet both did this.
- `events-sealed-hierarchy`: a sealed JSON event without `@JsonTypeInfo` published fine; no consumer could read it.

Writing tests for every cross-cutting concern is exactly the work agents skip and humans forget. Prefab already
knows the full model at compile time: every endpoint and its security, every event, its serializer and every handler
that consumes it, every migration. It can generate a small conformance suite that checks the wiring, not the
business logic, and runs on the in-memory backend in seconds.

Candidate checks (to be confirmed in analysis):
1. Every published event type round-trips through the platform serializer into every consumer's parameter type
   (sealed hierarchies, Avro, migrations included).
2. Every endpoint answers 401/403 without credentials unless it is declared public, and a request with a valid token
   reaches the generated controller (catches a replaced filter chain).
3. Every aggregate can be created through its generated `@Create` endpoint from the generated mother and read back.
4. Database migrations apply to an empty schema (containers mode only).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Analysis in this task picks the checks, records what each one would have caught in the benchmark results, and how the suite is generated (test sources, one class per concern) and opted out of per aggregate or event
- [ ] #2 The generated suite runs with a plain `mvn verify`, needs no hand-written code, and runs on the in-memory backend in a few seconds for the showcase
- [x] #3 A failure names the endpoint or event, what was expected and the likely cause (for example "Prefab's SecurityFilterChain was replaced by `MyConfig.publicChain`")
- [ ] #4 Re-running the benchmark tasks `security-public-endpoints` and `events-sealed-hierarchy` with the faulty solutions from the recorded runs fails the generated suite
- [x] #5 Documented in `generated-artefacts.md` and `feature-guides.md`, and mentioned in the agent cheat sheet (TASK-344)
- [x] #6 Existing database migrations are statically checked against the schema replayed from earlier migrations; a `DROP COLUMN` for a column that does not exist fails compilation with a migration-specific diagnostic
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Analysis (2026-11-25):

Scope for the first conformance suite:

1. **REST security wiring, one generated test class per aggregate.** Exercise each generated endpoint without credentials and with a valid MockMvc test user, using the endpoint's effective `@Security` metadata to determine whether anonymous access is allowed. This catches `security-public-endpoints`: its extra catch-all `SecurityFilterChain` allows public reads but blocks authenticated writes, which ordinary public-read assertions and the agent's own tests miss.
2. **Event serialization, one generated test class per event.** Build every concrete record in a sealed event family from its generated Mother, then dispatch it through the in-memory platform codec and deserialize it as the declared event type. This catches `events-sealed-hierarchy`: publishing succeeds, but decoding the sealed supertype fails without `@JsonTypeInfo`.
3. **Database migration replay validation at compile time.** Prefab already replays versioned SQL migrations to build the prior schema before generating a migration. Reject invalid schema transitions while replaying, including dropping a nonexistent column; this does not need an integration test or a database container.

Generated tests are emitted to `target/prefab-test-sources` alongside the existing test helpers and registered by the project's existing test-source build-helper setup. Each aggregate/event gets its own test class so a failure identifies the owning model and endpoint/event. Add a `conformanceTest` opt-out on `@Aggregate` and `@Event` (default enabled); it disables that type's generated behavioral conformance test but does not suppress migration SQL validation. Exclude the candidate "create every aggregate and read it back" check from this first slice because it overlaps generated CRUD integration tests and tests application behavior beyond wiring. Exclude applying migrations to an empty real schema because it is containers-only and the selected migration invariant can be checked statically from migration history.
<!-- SECTION:PLAN:END -->

## Implementation Notes
<!-- SECTION:NOTES:BEGIN -->
- Added the default-enabled `conformanceTest` opt-out to `@Aggregate` and `@Event`.
- Added `ConformanceTestPlugin`: one generated security test class per aggregate checks anonymous and authenticated
  access for its exposed endpoints; one event test class per event binding round-trips each concrete record through
  the registered platform codec without invoking application handlers. Failures identify the aggregate/event and
  endpoint/topic.
- During compile-time migration replay, dropping an absent column now fails unless the statement uses
  `DROP COLUMN IF EXISTS`. The processor reports the migration filename and invalid transition.
- Updated generated-artefact, integration-test, database migration, cheat-sheet and testing-recipe documentation.
  Existing benchmark tasks `security-public-endpoints` and `events-sealed-hierarchy` exercise the two generated-test
  checks; the task descriptions already match the target behavior.
- The conformance generator must allow filesystem-backed application source roots even though `prefab-test` is
  test-scoped and therefore absent from the main compile classpath. In-memory compiler fixtures remain gated unless
  they provide the `IntegrationTest` marker, avoiding invalid generated sources in processor tests.
- After that guard correction, the showcase generated 22 conformance test classes and all 48 generated tests passed
  through the Maven `verify` lifecycle. An unfiltered clean verify remains blocked in this Windows environment by
  `AgentContextPluginTest.writesBelowTargetWhenOutputIsAFileSystemDirectory`, which constructs an invalid `/C:/...`
  path; the failure occurs in the processor test module before showcase verification.
- Validation passed: 14 processor tests including an actual failing compilation with `V2__broken.sql`, 48 generated
  showcase conformance tests, plus `RecipeDocumentationTest` and `WorkspaceFactoryTest`. The revised generator guard
  also passes its focused processor test.
- Acceptance criterion #4 remains unverified: the checked-in benchmark results contain summaries and log paths but
  not the faulty attempt worktrees, so the exact recorded solutions could not be rerun. Keep this task open until
  those source snapshots are available or the owner accepts equivalent reproductions.
<!-- SECTION:NOTES:END -->
