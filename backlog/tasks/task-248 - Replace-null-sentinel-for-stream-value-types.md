---
id: TASK-248
title: Replace null sentinel for stream value types
status: Done
assignee: []
created_date: '2026-06-08 07:21'
updated_date: '2026-06-08 07:26'
labels:
  - streams
  - refactor
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate places where null is used as a sentinel for value type resolution in stream-related code and implement safer alternatives without adding extra parameters to stream APIs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Identify null-based value-type sentinel usage in stream code paths.
- [x] #2 Replace null sentinel flow with an explicit typed abstraction that keeps current stream method signatures unchanged.
- [x] #3 Run the streams module tests to verify no regressions in stream topology behavior.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Refactored `KafkaPrefabStream` to remove `null` as runtime value-type sentinel by introducing an internal `ValueTypeHint<T>` abstraction.

Key changes:
- Replaced `Class<V> valueType` with `ValueTypeHint<V>`.
- Added explicit `wrapKnown(...)` and `wrapUnknown(...)` helpers; map/flatMap/breakout/process/join now use unknown hint instead of `null`.
- Updated join deserialization path to consume `ValueTypeHint` and branch on known vs unknown runtime type.
- Updated merge known-type resolution to return `ValueTypeHint` rather than nullable classes.

Validation:
- Ran: `mvn -pl streams -am test -DskipITs`
- Result: BUILD SUCCESS (Prefab Core + Prefab Streams DSL tests passed).
<!-- SECTION:NOTES:END -->
