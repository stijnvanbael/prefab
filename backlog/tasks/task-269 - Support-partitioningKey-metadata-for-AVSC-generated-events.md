---
id: TASK-269
title: Support partitioningKey metadata for AVSC-generated events
status: To Do
assignee: []
created_date: '2026-07-27 08:36'
updated_date: '2026-07-27 08:36'
labels:
  - annotation-processor
  - avro
  - events
  - kafka
dependencies: []
priority: medium
---

## Description

When an event contract uses `@Avsc`, the generated records cannot declare `@PartitioningKey` themselves, so there is currently no developer-facing way to configure a partitioning key for AVSC-generated event types. Add support for defining partitioning-key metadata on `@Avsc` so each referenced schema can declare which generated property should be used as the event partitioning key.

Prefer a shape that lives next to `value()` and supports multi-schema contracts explicitly, for example a list of entries where each entry declares the AVSC file and the property name to use as the partitioning key.

## Acceptance Criteria

- [ ] #1 `@Avsc` exposes a developer-facing way to configure a partitioning key for each referenced AVSC schema without editing the generated event classes
- [ ] #2 The configuration supports both single-schema and multi-schema `@Avsc` usage, and the mapping from AVSC file to generated property name is explicit and unambiguous
- [ ] #3 The annotation processor reports clear compile-time errors when the configuration references an AVSC file that is not listed in `value()`, a property that does not exist in the corresponding schema, or duplicate/conflicting mappings for the same schema
- [ ] #4 Generated event registration and producer infrastructure uses the configured property as the partitioning key extractor for AVSC-generated events wherever Prefab currently honours event partitioning keys
- [ ] #5 Documentation and at least one example show the final annotation shape and explain the fallback behaviour when no AVSC partitioning key is configured

## Implementation Notes

- Keep non-`@Avsc` event behaviour unchanged; this task is only about closing the AVSC gap.
- Reuse the existing event registry and partitioning-key extraction flow instead of introducing a separate AVSC-specific mechanism.
- The final API shape should be a nested annotation such as `files = { @AvscFile(path = "...", keyProperty = "...") }`
- If the final API shape materially affects the public annotation contract, capture the decision in `backlog/decisions/`.
