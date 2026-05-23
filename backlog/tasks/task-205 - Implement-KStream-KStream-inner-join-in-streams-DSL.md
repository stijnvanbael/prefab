---
id: TASK-205
title: Implement KStream-KStream inner join in streams DSL
status: In Progress
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-23 08:58'
labels:
  - feature
  - streams
  - kafka
  - join
milestone: m-2
dependencies:
  - TASK-204
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 28.99169921875
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add Kafka-backed KStream-KStream inner join support with explicit window configuration and runnable coverage in the streams example.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL supports KStream-KStream inner join composition
- [ ] #2 Kafka backend maps join semantics to native Kafka Streams windowed inner join operations
- [ ] #3 `examples/streams` includes a runnable KStream-KStream inner join example
- [ ] #4 Tests cover matching keys, non-matching keys, and out-of-window events for the join
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Define the DSL join contract in `PrefabStream`:
   - Add `<VO, VR> PrefabStream<VR> join(PrefabStream<VO> other, JoinWindow window, BiFunction<? super V, ? super VO, ? extends VR> joiner)`.
   - Document that this is a KStream-KStream inner join with explicit window semantics.

2. Introduce explicit join window configuration type in the `streams` module:
   - Create `JoinWindow` (record) with `Duration timeDifference` and `Duration grace`.
   - Add static factory `of(Duration timeDifference, Duration grace)` with validation (non-null, non-negative values).
   - Keep naming join-agnostic so the type can be reused across future join variants.

3. Implement Kafka backend mapping in `KafkaPrefabStream`:
   - Implement `join(...)` by delegating to native `KStream.join(...)`.
   - Map window to `JoinWindows.ofTimeDifferenceAndGrace(...)`.
   - Use `StreamJoined.with(Serdes.String(), leftSerde, rightSerde)` with dynamic serde adapters.
   - Validate join context compatibility (same builder/resolver/serde context) similar to merge safeguards.

4. Add streams topology tests for join semantics in `KafkaPrefabStreamsTopologyTest`:
   - Matching keys within window emits joined output.
   - Non-matching keys emit nothing.
   - Matching keys outside window emit nothing.
   - (Optional hardening) reverse arrival order and boundary-edge assertions.

5. Add runnable join example in `examples/streams`:
   - Extend `StreamTopologyConfiguration` with a dedicated KStream-KStream inner join topology.
   - Add example event types for right-side input and joined output.
   - Update topic properties in `application.yml` and test config as needed.

6. Add integration coverage in `StreamsExampleApplicationTest`:
   - Publish left/right records and assert joined output for in-window matching keys.
   - Keep the existing branch/merge scenario intact.

7. Documentation updates:
   - Update `examples/streams/README.md` with join usage and run instructions.
   - Update `backlog/docs/feature-guides.md` streams section to include the new join operator.

8. Verification:
   - Run `mvn -pl streams test`.
   - Run `mvn -pl examples/streams -am test`.
   - Resolve failures and ensure deterministic timing assertions for window tests.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented initial code changes in `streams` module: added DSL `join(...)`, introduced `JoinWindow`, Kafka backend join mapping, and topology tests for matching/non-matching/out-of-window scenarios.

Verified module tests with `mvn -pl streams test` (pass). Remaining work includes runnable `examples/streams` join scenario, integration test coverage, and docs updates.
<!-- SECTION:NOTES:END -->
