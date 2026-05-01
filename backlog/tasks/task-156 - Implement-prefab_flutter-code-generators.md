---
id: TASK-156
title: Implement prefab_flutter code generators
status: To Do
assignee: []
created_date: '2026-05-01 18:04'
updated_date: '2026-05-01 18:04'
labels:
  - flutter
  - code-generation
dependencies:
  - TASK-155
priority: high
ordinal: 156000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement the `prefab_flutter` Dart package — the `build_runner` plugin that reads
`@PrefabView`-annotated Dart classes and generates all UI artefacts.

The generator skeleton is in `flutter/prefab_flutter/` from the spike (TASK-154). This task
implements each sub-generator fully:

- **`ListScreenGenerator`** — responsive layout (mobile ListView / desktop DataTable), `@PrefabParent`
  nesting, `PrefabPage<T>` pagination model, search bar, sort controls, FAB, swipe-to-delete
- **`FormScreenGenerator`** — type-based widget selection (`String`→TextField, `double`→numeric,
  `bool`→Switch, `DateTime`→DatePicker, `enum`→Dropdown), cross-field validation, file upload
- **`ProviderGenerator`** — full CRUD lifecycle, error boundary, loading state
- **`ApiClientGenerator`** — authentication header injection, error mapping, retry on 5xx
- **`RoutesGenerator`** — nested parent routes, `$prefabRoutes` aggregation list
- **`DetailScreenGenerator`** *(new)* — read-only detail view with edit / delete actions
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All six generators are implemented and pass their unit tests using `build_test` / `source_gen` test utilities
- [ ] #2 Running `flutter pub run build_runner build` on `prefab_flutter_example` produces syntactically and semantically valid Dart output
- [ ] #3 `ListScreenGenerator` produces a responsive layout that works on mobile (ListView) and desktop/tablet (DataTable)
- [ ] #4 `FormScreenGenerator` selects the correct widget for each Dart primitive type and respects `@FormField(widget:)` overrides
- [ ] #5 `ProviderGenerator` wraps all API calls in error handling and exposes loading state
- [ ] #6 Generated code compiles without errors alongside `freezed`- and `riverpod_generator`-generated code
- [ ] #7 Package passes `dart pub publish --dry-run`
<!-- AC:END -->
