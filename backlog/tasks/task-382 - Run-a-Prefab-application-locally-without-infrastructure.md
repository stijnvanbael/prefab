---
id: TASK-382
title: Run a Prefab application locally without infrastructure
status: In Progress
assignee: []
created_date: '2026-11-24 10:00'
labels:
  - dx
  - agents
  - productivity
dependencies:
  - TASK-175
priority: high
type: feature
ordinal: 203000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-175 puts `@IntegrationTest` on an in-memory backend. The same backend can serve the application itself: one
command starts the app with in-memory repositories and the in-process event bus, no Docker, no broker, no database.

Why it matters:
- **Agents verify behaviour the way a user sees it.** Today an agent's only feedback is compile errors and the tests
  it writes itself. In the benchmark, the two "agent tested only one side" failures (`security-public-endpoints`,
  `events-sealed-hierarchy`, see `benchmark/results/*/classifications.yml`) would have shown up with one `curl`
  against the running app. Starting the app currently needs Postgres/Mongo plus Kafka/Pub/Sub/SNS-SQS.
- **Humans get an instant demo loop**: Swagger UI and AsyncAPI docs on a running app seconds after writing an
  aggregate, the "wow" moment for adoption, and a base for TASK-186 (playground).
- **Frontend developers** can develop against the backend without running its infrastructure.

Spring Boot supports this pattern with a test-scoped launcher (`SpringApplication.from(App::main).with(...)`, run
with `spring-boot:test-run`). Prefab knows the application class and the active plugins, so it can generate that
launcher in test sources, or ship it in `prefab-test`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 One documented command starts any Prefab application on the in-memory backend with Docker unavailable (for example `mvn spring-boot:test-run`), without hand-written configuration
- [x] #2 Events published by the running app reach its own `@EventHandler`s through the platform serializers, as in TASK-175
- [x] #3 Swagger UI and AsyncAPI docs are reachable when their modules are on the classpath; security works with a documented local token or a dev-only permissive mode that cannot be activated in production
- [x] #4 Optional seed data at startup (for example from a `src/test/resources` file or the generated mothers of TASK-102), so a fresh app is not empty
- [x] #5 The startup log lists the base URL, the docs URLs and which backends are in memory, and the per-project agent context (TASK-345) and agent cheat sheet (TASK-344) tell agents to use this to check their work
- [x] #6 The showcase starts this way in CI as a smoke test; documented in `getting-started.md` and `feature-guides.md`
- [ ] #7 A benchmark context or preamble variant that mentions local runs is compared against the baseline (TASK-343), recording turns, builds and pass rate
<!-- AC:END -->

## Analysis

<!-- SECTION:ANALYSIS:BEGIN -->
Use Spring Boot's `spring-boot:test-run` goal rather than generating a launcher: Spring Boot 4.2 is already the
project baseline, and `prefab-test` is already available on application test classpaths. Its auto-configurations
replace persistence and messaging when `prefab.test.containers=false`, so local execution should reuse the same
backend and serializers as TASK-175 without changing generated artefacts or production dependencies.

The local-run path should remain test-scoped. It must not add a permissive security mode to production; document the
showcase's existing `local` profile, which uses HTTP Basic and Spring Boot's generated development password, and keep
the default JWT setup for production profiles. Optional seed data is app-specific, so demonstrate it with a
test-source `ApplicationRunner` and generated repositories instead of adding a framework-wide seed format.

Add a startup banner only for a running in-memory web server, showing the bound URL, docs routes available from
OpenAPI/AsyncAPI modules, and the in-memory persistence/event backends. Verify event handling through the existing
serializer round-trip in the showcase runtime smoke test. Update the developer guide, agent cheat sheet, showcase
CI coverage, and add an isolated benchmark context; a model-run comparison can only be recorded if the benchmark
runner and credentials are available in this environment.
<!-- SECTION:ANALYSIS:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Added a test-scoped `prefab-test` auto-configuration that logs the actual server URL, classpath-available OpenAPI/Swagger and AsyncAPI routes, and detected in-memory repository/event-bus backends when a web server is ready.
- The `showcase-inmemory` variant defaults to its existing `local` profile. A `local-demo` runner in test sources seeds one category via the generated repository. The runtime smoke test checks the seed, serves OpenAPI/Swagger/AsyncAPI endpoints, and verifies customer registration reaches the order-history handler through the in-memory serialized event bus.
- Documented the `spring-boot:test-run` workflow, local authentication, optional test-only seed profile, and live HTTP verification in the getting-started guide, feature guide, recipe, agent cheat sheet, generated agent context, and benchmark context.
- Verification passed: `mvn -f examples/showcase-inmemory/pom.xml -Dtest=LocalApplicationSmokeTest test` (1 test); `mvn -pl benchmark "-Dtest=RecipeDocumentationTest,WorkspaceFactoryTest" test` (5 tests); targeted `AgentContextPluginTest` (1 test); and `git diff --check`. A live `spring-boot:test-run` with `local,local-demo` also started and logged the base URL and in-memory backends.
- AC #7 remains open. The `local-run` benchmark context is discoverable, but the benchmark agent CLI (`claude`) is unavailable here, so no comparable model run or turns/builds/pass-rate result can be recorded. The existing cheat-sheet baseline is not a like-for-like comparison.
<!-- SECTION:NOTES:END -->
