---
id: TASK-253
title: Fix KafkaPrefabStreamsTopologyTest to work with JSON-serialized keys
status: To Do
assignee: []
created_date: '2026-06-19 06:48'
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
- [ ] #1 KafkaPrefabStreamsTopologyTest runs with 0 test failures
- [ ] #2 All topology test methods correctly read output values (with or without full key deserialization as appropriate)
<!-- AC:END -->
