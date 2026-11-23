---
id: TASK-214
title: Add keyBy and repartition operators with explicit topic naming
status: Done
assignee: []
created_date: '2026-05-17 09:37'
updated_date: '2026-05-21 06:38'
labels:
  - feature
  - streams
  - kafka
milestone: m-3
dependencies:
  - TASK-202
  - TASK-203
  - TASK-207
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 72.479248046875
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Introduce `keyBy(...)` and explicit `repartition(...)` DSL operators so users can control re-keying behavior and repartition topic names.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL exposes `keyBy(...)` and `repartition(...)` operators
- [x] #2 When explicitly configured, generated topology uses the exact repartition topic name provided by the user
- [x] #3 Validation rejects invalid or conflicting repartition topic names with actionable error messages
- [x] #4 Integration tests verify correct re-keying behavior and named repartition topic usage
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Execution sequencing note: start TASK-214 after TASK-207.

Preferred order in M4 lane: TASK-207 -> TASK-214 -> TASK-216 to stabilize key distribution and repartition conventions before custom processors.

Parallelization: fixture and contract prep can run earlier, but operator wiring should follow TASK-207 completion.

Implemented: `keyBy(keyType, mapper[, name])`, `repartition([name])` on PrefabStream; Kafka impl uses `KStream.repartition(Repartitioned.withName)` (Kafka adds app-id prefix and `-repartition` suffix). Names validated in StreamStepNames (non-blank, [a-zA-Z0-9._-], <=200 chars, unique per topology; conflicts throw IllegalArgumentException). Unnamed ops get generated names (repartition-<type>). Tests in KafkaPrefabStreamsTopologyTest.
<!-- SECTION:NOTES:END -->
