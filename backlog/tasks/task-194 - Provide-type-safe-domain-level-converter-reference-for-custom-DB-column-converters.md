---
id: TASK-194
title: >-
  Provide type-safe domain-level converter reference for custom DB column
  converters
status: To Do
assignee: []
created_date: '2026-05-11 17:01'
updated_date: '2026-05-21 06:22'
labels:
  - feature-request
  - architecture
  - converter
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/maestro/src/main/java/be/appify/maestro/domain/memory/MemoryEntry.java
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/maestro/src/test/java/be/appify/maestro/ArchitectureTest.java
priority: medium
ordinal: 164000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Maestro currently needs to import infrastructure converter class `FloatArrayToVectorConverter` directly in domain aggregate `MemoryEntry` to satisfy `@DbColumn(converter = ...)`, which violates strict domain->infrastructure layering in ArchUnit. Add a Prefab feature to allow a type-safe converter reference that can live in a neutral/shared package (or be resolved by alias), so domain model no longer imports infrastructure types.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A domain aggregate can reference a custom converter without importing infrastructure package classes
- [ ] #2 Generated repository/runtime still applies converter correctly
- [ ] #3 Migration path documented for existing `@DbColumn(converter = ...)` usage
<!-- AC:END -->
