---
id: TASK-374
title: Run a Prefab application locally without infrastructure
status: To Do
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
- [ ] #1 One documented command starts any Prefab application on the in-memory backend with Docker unavailable (for example `mvn spring-boot:test-run`), without hand-written configuration
- [ ] #2 Events published by the running app reach its own `@EventHandler`s through the platform serializers, as in TASK-175
- [ ] #3 Swagger UI and AsyncAPI docs are reachable when their modules are on the classpath; security works with a documented local token or a dev-only permissive mode that cannot be activated in production
- [ ] #4 Optional seed data at startup (for example from a `src/test/resources` file or the generated mothers of TASK-102), so a fresh app is not empty
- [ ] #5 The startup log lists the base URL, the docs URLs and which backends are in memory, and the per-project agent context (TASK-345) and agent cheat sheet (TASK-344) tell agents to use this to check their work
- [ ] #6 The showcase starts this way in CI as a smoke test; documented in `getting-started.md` and `feature-guides.md`
- [ ] #7 A benchmark context or preamble variant that mentions local runs is compared against the baseline (TASK-343), recording turns, builds and pass rate
<!-- AC:END -->
