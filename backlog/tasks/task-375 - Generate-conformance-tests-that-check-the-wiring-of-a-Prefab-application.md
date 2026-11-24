---
id: TASK-375
title: Generate conformance tests that check the wiring of a Prefab application
status: To Do
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
- [ ] #1 Analysis in this task picks the checks, records what each one would have caught in the benchmark results, and how the suite is generated (test sources, one class per concern) and opted out of per aggregate or event
- [ ] #2 The generated suite runs with a plain `mvn verify`, needs no hand-written code, and runs on the in-memory backend in a few seconds for the showcase
- [ ] #3 A failure names the endpoint or event, what was expected and the likely cause (for example "Prefab's SecurityFilterChain was replaced by `MyConfig.publicChain`")
- [ ] #4 Re-running the benchmark tasks `security-public-endpoints` and `events-sealed-hierarchy` with the faulty solutions from the recorded runs fails the generated suite
- [ ] #5 Documented in `generated-artefacts.md` and `feature-guides.md`, and mentioned in the agent cheat sheet (TASK-344)
<!-- AC:END -->
