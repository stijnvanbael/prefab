---
id: TASK-253
title: Fix KafkaPrefabStreamsTopologyTest to work with JSON-serialized keys
status: Done
assignee: []
created_date: '2026-06-19 06:48'
updated_date: '2026-06-19 06:55'
labels:
  - streams
  - testing
  - follow-up
dependencies:
  - TASK-252
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Task TASK-252 implemented JSON-based key serialization for Kafka Streams, which automatically serializes Key types to JSON. The streams topology tests now fail when reading output topics because the test infrastructure needs to be updated to match the new key serde behavior.

The tests use TopologyTestDriver with test input/output topics that now have JsonKeySerde for keys, but some tests fail to deserialize Output Topic reads. This appears to be due to either:
1. Mismatched serdes between what the topology uses internally (e.g., DeferredJsonKeySerde for stores) and what the test tries to read
2. Tests expecting string-based key handling that no longer applies

All 40+ topology tests in KafkaPrefabStreamsTopologyTest need to be reviewed and refactored to either:
- Use properly-typed input/output methods with correct Key<K> types
- Or refactored to use rawOutput() for value-only assertions if keys don't matter

Error pattern: "Unrecognized token 'Reference'" when trying to deserialize JSON-encoded keys.

See: /Users/stijnvanbael/IdeaProjects/appify/prefab/streams/src/test/java/be/appify/prefab/streams/kafka/KafkaPrefabStreamsTopologyTest.java
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 KafkaPrefabStreamsTopologyTest runs with 0 test failures
- [x] #2 All topology test methods correctly read output values (with or without full key deserialization as appropriate)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Investigating remaining 7 failures in KafkaPrefabStreamsTopologyTest after JSON key serde migration.

Root cause confirmed: KafkaPrefabStream still used StringKeySerde for join repartition topics and terminal to(...) sinks, producing non-JSON key bytes (e.g., Reference[...] string form) while readers expected JsonKeySerde. Fixed by switching those paths to JsonKeySerde and propagating injected JsonMapper into KafkaPrefabStream.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed all 7 failing tests in KafkaPrefabStreamsTopologyTest.

What changed:
- Updated `KafkaPrefabStream` to use `JsonKeySerde` (instead of `StringKeySerde`) for:
  - join internals (`StreamJoined.with(...)` key serde)
  - sink writes (`to(String)` via `Produced.with(...)` key serde)
- Added `JsonMapper` as a required dependency of `KafkaPrefabStream` and propagated it through constructors/wrap methods.
- Updated `KafkaPrefabStreams` to pass the injected `JsonMapper` when creating `KafkaPrefabStream`.

Why this fixed it:
- The topology had mixed key wire formats: sources/stores used JSON keys, but join/sink paths still emitted string-based keys. Tests reading with `JsonKeySerde` then failed with `Unrecognized token 'Reference'`.
- Using JSON key serde consistently across source, join/repartition, stores, and sink removes the format mismatch.

Validation:
- `mvn -pl streams -Dtest=KafkaPrefabStreamsTopologyTest test` => PASS (exit 0)
- `mvn -pl streams test` => PASS (exit 0)

Result:
- `KafkaPrefabStreamsTopologyTest` now has 0 failures.
<!-- SECTION:FINAL_SUMMARY:END -->
