---
id: TASK-296
title: 'Fuzzy matching for the FULL_TEXT filter'
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
Make `@Filter(operator = FULL_TEXT)` tolerant to typos. Builds on the executors introduced in TASK-030 (`FullTextCondition` gets a match mode) and on the pg_trgm support already generated for `@Autocomplete` (TASK-246). PostgreSQL: combine the tsvector match with trigram similarity (`%` / `word_similarity`) and generate the trigram index. MongoDB: `$text` has no fuzziness; decide between a documented limitation, a regex fallback, or Atlas Search when available.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Opt-in attribute on @Filter (for example fuzzy = true or a match mode) that keeps exact full-text behaviour as the default
- [x] #2 PostgreSQL: a query with a typo still finds the intended rows; pg_trgm extension and trigram indexes are generated in the migration (reuse the TASK-246 mechanism)
- [x] #3 Similarity threshold is configurable with a sensible default
- [x] #4 MongoDB behaviour is decided and documented (supported, degraded or rejected at compile time)
- [x] #5 Works together with range, example, tenant and parent filters through FullTextQueryExecutor
- [x] #6 Tested on PostgreSQL and MongoDB; Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

- `@Filter(fuzzy, similarityThreshold=0.3)` -> `FullTextCondition(properties, query, fuzzy, similarityThreshold)`; exact stays default.
- PostgreSQL: `tsvector @@ websearch_to_tsquery OR word_similarity(:q, col) >= :threshold` per column; migration enables pg_trgm and adds trigram GIN indexes. The function-call predicate does not use the trigram index (documented).
- MongoDB: degraded - fuzzy is ignored, search stays exact (documented, unit-tested with mocks as no Mongo testcontainer exists).
- Compile error for thresholds outside (0, 1].
- Tests: FullTextFilterTest, DbMigrationWriterTest, JdbcFullTextQueryExecutorTest (Testcontainers), MongoFullTextQueryExecutorTest. Full build: all modules pass up to examples; avro-example fails only because the Kafka Docker image could not be pulled (environment).
