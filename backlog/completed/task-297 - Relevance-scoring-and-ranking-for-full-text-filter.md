---
id: TASK-297
title: 'Relevance scoring and ranking for the FULL_TEXT filter'
status: Done
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
- [x] #1 Results can be ordered by relevance, descending, via the Sort of the Pageable (reserved property name documented)
- [x] #2 Default ordering is by relevance when a search text is given and no explicit sort was requested
- [x] #3 Fields can be given a weight (for example @Filter(weight = ...)); PostgreSQL uses setweight A-D, MongoDB uses text index weights
- [x] #4 The score is not exposed in the response in this story; exposing it is out of scope
- [x] #5 Generated index matches the weighted expression so it stays usable
- [x] #6 Tested on PostgreSQL and MongoDB; Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

- Reserved sort property `relevance` (`FullTextCondition.RELEVANCE`), always descending; default order when a search text is given and the sort is empty; explicit sort wins.
- `@Filter(weight = Filter.Weight.A..D)` flows through `FullTextCondition.weights`. `TsVectorExpression` (core) builds the tsvector expression shared by the generated GIN index and the `JdbcFullTextQueryExecutor` (`ts_rank_cd`); weights only change the expression when a non-default weight is used. MongoDB: text index field weights, `TextQuery.sortByScore()`.
- Score is not exposed. Tests: JdbcFullTextQueryExecutorTest (Testcontainers), MongoFullTextQueryExecutorTest (query shape), DbMigrationWriterTest, FullTextFilterTest. Docs: annotation-reference.md.
- Verification: core, processor, postgres, mongodb modules green; avro example failed only on a Docker image pull timeout (environmental).
