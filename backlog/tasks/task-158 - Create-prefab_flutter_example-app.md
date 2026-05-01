---
id: TASK-158
title: Create prefab_flutter_example app
status: To Do
assignee: []
created_date: '2026-05-01 18:04'
updated_date: '2026-05-01 18:04'
labels:
  - flutter
  - example
dependencies:
  - TASK-156
  - TASK-157
priority: medium
ordinal: 158000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Build out the `prefab_flutter_example` Flutter application — an end-to-end showcase of Prefab
Flutter that works against the existing Prefab Java example backend (the `examples/kafka` or a
dedicated REST example).

The example should demonstrate at least three entities:
- `Product` — basic CRUD with list, detail, create, edit, delete
- `Category` — CRUD with a `@ListColumn(searchable: true)` search field
- `OrderLine` — nested entity using `@PrefabParent` referencing `Order`

The app must run on Android, iOS, and web (Flutter multi-platform).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 App builds and runs on Android, iOS, and web without errors
- [ ] #2 All three example entities have fully working list, detail, create, edit, and delete flows end-to-end against the backend
- [ ] #3 `OrderLine` nested entity demonstrates `@PrefabParent` nesting with routes `/orders/:orderId/order-lines`
- [ ] #4 App includes a `README.md` with setup instructions (how to start the backend, how to run the app)
- [ ] #5 Generated `.prefab.g.dart` files are committed so reviewers can see the output without running `build_runner`
<!-- AC:END -->
