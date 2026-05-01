---
id: TASK-159
title: Prefab Flutter — backend integration via OpenAPI spec
status: To Do
assignee: []
created_date: '2026-05-01 18:04'
updated_date: '2026-05-01 18:04'
labels:
  - flutter
  - openapi
  - integration
dependencies:
  - TASK-156
priority: low
ordinal: 159000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When the backend is a Prefab Java project, the Flutter app should be able to derive `@PrefabApi`
paths, field types, and server-side validation rules directly from the generated OpenAPI spec —
eliminating duplication between the backend model and the Flutter model annotations.

This task investigates and implements a Prefab Flutter `build_runner` plugin that:

1. Reads the OpenAPI spec (from a configurable URL or file path)
2. Generates a `prefab_flutter_api_config.dart` file containing `const` maps of API paths and
   field metadata
3. Optionally generates stub Dart model classes annotated with `@PrefabView` so the developer
   only needs to add UI-specific annotations (`@ListColumn`, `@FormField`) rather than writing
   the full model
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `prefab_flutter_openapi` builder reads a local or remote OpenAPI spec and generates `@PrefabApi`-annotated model stubs
- [ ] #2 Field types are correctly mapped from OpenAPI types to Dart types (`string`→`String`, `number`→`double`, `boolean`→`bool`, etc.)
- [ ] #3 Server-side validation constraints (`required`, `minLength`, `pattern`) are mapped to `@FormField(validators: [...])` equivalents
- [ ] #4 The example app demonstrates the integration with the Prefab Java example backend's OpenAPI spec
<!-- AC:END -->
