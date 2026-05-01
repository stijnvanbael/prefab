---
id: TASK-157
title: Create prefab_flutter_widgets runtime library
status: To Do
assignee: []
created_date: '2026-05-01 18:04'
updated_date: '2026-05-01 18:04'
labels:
  - flutter
  - widgets
dependencies:
  - TASK-156
priority: medium
ordinal: 157000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create the `prefab_flutter_widgets` Dart package — a small Flutter widget library that provides
the shared runtime components referenced by generated code:

- `PrefabSearchBar` — debounced search input that calls a callback on change
- `PrefabPaginationBar<T>` — previous / next page controls wired to the entity's `AsyncNotifier`
- `PrefabSortMenu<T>` — app-bar action menu listing sortable columns from `@ListColumn(sortable: true)`
- `PrefabDeleteDialog<T>` — confirmation dialog that shows `@PrefabDelete.confirmMessage`
- `PrefabPage<T>` — standard pagination envelope (`items`, `page`, `size`, `totalElements`, `totalPages`)

These widgets must be pure Material 3 so they respect the app's `ThemeData` without any
Prefab-specific styling.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All five widgets are implemented and have widget tests covering their key interactions
- [ ] #2 `PrefabPage<T>` serialises from the standard Spring Page JSON format (`content`, `number`, `size`, `totalElements`, `totalPages`)
- [ ] #3 All widgets are pure Material 3 and pass Flutter's accessibility checks
- [ ] #4 Package passes `dart pub publish --dry-run`
<!-- AC:END -->
