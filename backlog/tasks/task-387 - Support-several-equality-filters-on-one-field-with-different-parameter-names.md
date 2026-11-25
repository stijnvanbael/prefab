---
id: TASK-387
title: Support several equality filters on one field with different parameter names
status: To Do
assignee: []
created_date: '2026-11-25 06:05'
labels:
  - rest
  - processor
dependencies: []
priority: medium
type: bug
ordinal: 212000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Two `@Filter(operator = EQUAL)` annotations on one field, the second with `parameter = "brand"`, generate code that
does not compile: `ProductQuery`, `ProductService.getList` and `ProductController.getList` declare the field's name
twice ("variable manufacturer is already defined"). The `parameter` attribute is ignored for the second filter.

Use case (found while writing the TASK-377 brownfield task `brownfield-rename-contract`): after renaming
`brand` to `manufacturer`, old clients keep filtering with `?brand=` while new clients use `?manufacturer=`.

Either generate one request parameter per filter, named after its `parameter`, or reject the combination with a clear
processor error.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Two filters with the same operator and different `parameter` names on one field compile and both filter the list
- [ ] #2 Two filters that would map to the same request parameter fail with a processor error naming the field
<!-- AC:END -->
