---
id: TASK-249
title: Provide stable representative names for Kafka stream steps
status: Done
assignee: []
created_date: '2026-06-12 05:15'
updated_date: '2026-06-12 05:26'
labels:
  - streams
  - kafka
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace unstable random Kafka Streams step names with unique, representative, and stable names so generated topologies are deterministic and easier to inspect.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Kafka stream branch step names are stable across runs for the same DSL structure.
- [x] #2 Generated step names are representative of the stream operation rather than random UUIDs.
- [x] #3 Step naming remains unique within a topology to avoid Kafka Streams naming collisions.
- [x] #4 Relevant tests cover the stable naming behaviour.
- [x] #5 Relevant developer documentation is updated if public or user-observable behaviour changes.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Replaced the random UUID-based branch naming in `KafkaPrefabStream` with a shared `StreamStepNames` helper that issues deterministic `branch-N` names per Kafka streams topology context. The branch output now uses Kafka Streams' branch name concatenation intentionally (`Branched.as("-matched")`) so the resulting processor names are stable and representative, such as `branch-1-matched`. Added topology regression tests for deterministic naming and uniqueness, and documented the user-visible naming behaviour in `backlog/docs/feature-guides.md`.

Full `mvn test` revealed an unrelated pre-existing compile failure in `examples/streams/src/main/java/be/appify/prefab/example/streams/StreamTopologyConfiguration.java`; tracked separately as TASK-250.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented stable, representative Kafka Streams branch step names by replacing UUID-based names with deterministic per-topology branch counters, added regression coverage for topology descriptions, and documented the naming behaviour in the streams feature guide.
<!-- SECTION:FINAL_SUMMARY:END -->
