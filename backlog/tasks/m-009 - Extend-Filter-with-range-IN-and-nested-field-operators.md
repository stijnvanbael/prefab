---
id: M-009
title: 'Extend @Filter with range, IN, and nested field operators'
status: To Do
assignee: []
created_date: '2026-05-08 16:38'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Extend `@Filter` beyond the current string-match operators to support range comparisons, collection membership (`IN`), and nested field paths. This reduces the need to reach for `@RepositoryMixin` for common query patterns.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 @Filter supports IN operator (matches any value in a provided list)
- [ ] #2 @Filter supports GT, GTE, LT, LTE operators for numeric and date fields
- [ ] #3 @Filter supports nested field filtering via dot notation (e.g. @Filter("address.city"))
- [ ] #4 All new operators are tested with PostgreSQL and MongoDB backends
- [ ] #5 Developer Guide updated with all new operators and examples
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
