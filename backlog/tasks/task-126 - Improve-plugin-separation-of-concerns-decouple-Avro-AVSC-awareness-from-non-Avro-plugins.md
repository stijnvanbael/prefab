---
id: TASK-126
title: >-
  Improve plugin separation of concerns: decouple Avro/AVSC awareness from
  non-Avro plugins
status: To Do
assignee: []
created_date: '2026-04-18 08:59'
updated_date: '2026-04-24 06:57'
labels:
  - refactoring
  - architecture
  - plugin
dependencies: []
priority: medium
ordinal: 140000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The plugin architecture has a separation-of-concerns violation: AVSC/Avro-specific concerns bleed into unrelated plugins (MotherPlugin, EventSchemaDocumentationPlugin, ConsumerWriterSupport) and into PrefabContext itself. MotherPlugin checks isAvscGeneratedRecord() to pick the right source element. EventSchemaDocumentationPlugin has its own resolveAvscEvents()/isAvscInterface() logic. ConsumerWriterSupport has hasAvscEventWithoutConcreteType() and @Avsc-specific branching. EventPlatformPluginSupport.isAvscGeneratedRecord() is a static helper called by any plugin that must treat AVSC-generated records differently. PrefabContext.eventElements() couples the shared context to Avsc-specific scanning. The root cause is that no abstraction lets AvscPlugin/AvroPlugin contribute their generated types to the rest of the system without the rest of the system having to know about @Avsc at all.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A plugin contribution API is introduced that allows AvscPlugin to register its generated record types once, without callers needing to know about @Avsc
- [ ] #2 PrefabContext.eventElements() no longer contains @Avsc-specific scanning logic; it delegates to registered plugin contributions
- [ ] #3 MotherPlugin no longer imports @Avsc or calls isAvscGeneratedRecord(); the correct source element is provided transparently
- [ ] #4 EventSchemaDocumentationPlugin no longer contains resolveAvscEvents() or isAvscInterface(); AVSC expansion is owned by AvscPlugin
- [ ] #5 ConsumerWriterSupport no longer contains hasAvscEventWithoutConcreteType() or @Avsc-specific branching; deferral is based on type availability, not @Avsc presence
- [ ] #6 EventPlatformPluginSupport.isAvscGeneratedRecord() is removed or made private to the avro package
- [ ] #7 Static helpers (isLogicalType, isNestedRecord, nestedTypes, sealedSubtypes) are moved from AvroPlugin into a dedicated AvroTypeSupport class, removing static-import coupling in writer classes
- [ ] #8 All existing tests pass with no behavioural changes
<!-- AC:END -->
