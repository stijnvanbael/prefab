---
id: TASK-331
title: Evaluate faceted search query integration
status: To Do
assignee: []
created_date: '2026-11-18 10:38'
labels:
  - spike
  - search
  - architecture
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Assess whether a developer can expose search results with facets, relevance, highlighting and cursors without forcing search-index results into aggregate CRUD. Compare existing full-text filters and search ranking, repository mixins, TASK-319 projections and a search-specific adapter before proposing a provider or API. Related: TASK-309 and decision-006.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Demonstrate a bounded catalogue search scenario and compare current Prefab filters and mixins with a typed search-adapter prototype; distinguish implemented capabilities from proposed behavior.
- [ ] #2 Specify result and facet shapes, stable cursor semantics, ranking/highlight ownership, stale index and missing-record behavior, and backend portability limits.
- [ ] #3 Specify and demonstrate tenant and row authorization applied before hits, facets and counts are returned, with tests for isolation and a failed search dependency.
- [ ] #4 Document ownership, generated versus handwritten contracts, test strategy and a go/no-go recommendation; keep aggregate writes and existing CRUD unchanged.
<!-- AC:END -->
