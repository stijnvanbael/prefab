---
id: TASK-155
title: Implement prefab_flutter_annotations package
status: To Do
assignee: []
created_date: '2026-05-01 18:04'
updated_date: '2026-05-01 18:04'
labels:
  - flutter
  - annotations
dependencies:
  - TASK-154
priority: high
ordinal: 155000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Publish the `prefab_flutter_annotations` Dart package to pub.dev with a stable, versioned public API.

The package contains the annotation classes that developers place on their model classes to drive
Prefab Flutter code generation: `@PrefabView`, `@PrefabCreate`, `@PrefabUpdate`, `@PrefabDelete`,
`@PrefabApi`, `@ListColumn`, `@FormField`, `@Hidden`, `@PrefabParent`, and supporting enums
`Validator` and `FieldWidget`.

A skeleton and design for all annotation classes is in `flutter/prefab_flutter_annotations/` from
the spike (TASK-154). This task polishes, tests, and publishes that skeleton.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All annotation classes documented in `backlog/docs/prefab-flutter-design.md` §4 are implemented with full Dart doc-comments and at least one usage example
- [ ] #2 Package passes `dart pub publish --dry-run` without errors or warnings
- [ ] #3 Unit tests verify that annotation instances are correctly constructed with default and explicit values
- [ ] #4 Package is published to pub.dev under the `be.appify.prefab` / `prefab_flutter_annotations` identifier
- [ ] #5 `flutter/prefab_flutter_annotations/CHANGELOG.md` and `README.md` are written
<!-- AC:END -->
