---
id: TASK-297
title: 'Relevance scoring and ranking for the FULL_TEXT filter'
status: To Do
assignee: []
created_date: '2026-10-16 11:45'
labels:
  - "\U0001F4E6feature"
dependencies:
  - TASK-030
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow results of a `FULL_TEXT` filter to be ordered by relevance. PostgreSQL: `ts_rank_cd` over the same tsvector expression; MongoDB: `{$meta: "textScore"}`. Per-field weights (title more important than body) belong to this story. Requires a way to request score ordering through `Pageable`/`Sort` (for example a reserved sort property such as `relevance`) that executors translate, and an opt-in default of sorting by relevance when no explicit sort is given and a query is present.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Results can be ordered by relevance, descending, via the Sort of the Pageable (reserved property name documented)
- [ ] #2 Default ordering is by relevance when a search text is given and no explicit sort was requested
- [ ] #3 Fields can be given a weight (for example @Filter(weight = ...)); PostgreSQL uses setweight A-D, MongoDB uses text index weights
- [ ] #4 The score is not exposed in the response in this story; exposing it is out of scope
- [ ] #5 Generated index matches the weighted expression so it stays usable
- [ ] #6 Tested on PostgreSQL and MongoDB; Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
