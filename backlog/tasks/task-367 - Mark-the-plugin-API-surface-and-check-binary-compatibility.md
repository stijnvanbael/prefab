---
id: TASK-367
title: Mark the PrefabPlugin API surface and check binary compatibility
status: To Do
assignee: []
created_date: '2026-11-23 12:00'
labels:
  - annotation-processor
  - dx
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
decision-009 declares the `PrefabPlugin` hooks, `order()` / `EXECUTION_ORDER`, the member-conflict rule,
`PrefabContext` event discovery and the read accessors of the manifest types a provisional stable contract, and
everything else in the annotation processor internal. Nothing in the code tells plugin authors which is which, and
nothing stops a release from breaking a plugin by accident.

Make the boundary explicit and enforced, without moving packages in this story.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The plugin-facing types and members listed in decision-009 are marked as API (annotation or package documentation); public processor types outside that list are marked internal.
- [ ] #2 The build compares the marked API against the previous release and fails on binary-incompatible changes, with a documented way to acknowledge an intended break.
- [ ] #3 An ArchUnit test keeps built-in plugins and external plugin modules (`kafka`, `pubsub`, `sns-sqs`, `avro-processor`, `terraform`) from depending on internal processor types, or lists the remaining exceptions explicitly.
- [ ] #4 The extension point guide states the compatibility promise, including that the javapoet major version is part of it, and decision-009 is updated from "provisional" to "stable".
<!-- AC:END -->
