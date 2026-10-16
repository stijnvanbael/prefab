---
id: TASK-296
title: 'Fuzzy matching for the FULL_TEXT filter'
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
Make `@Filter(operator = FULL_TEXT)` tolerant to typos. Builds on the executors introduced in TASK-030 (`FullTextCondition` gets a match mode) and on the pg_trgm support already generated for `@Autocomplete` (TASK-246). PostgreSQL: combine the tsvector match with trigram similarity (`%` / `word_similarity`) and generate the trigram index. MongoDB: `$text` has no fuzziness; decide between a documented limitation, a regex fallback, or Atlas Search when available.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Opt-in attribute on @Filter (for example fuzzy = true or a match mode) that keeps exact full-text behaviour as the default
- [ ] #2 PostgreSQL: a query with a typo still finds the intended rows; pg_trgm extension and trigram indexes are generated in the migration (reuse the TASK-246 mechanism)
- [ ] #3 Similarity threshold is configurable with a sensible default
- [ ] #4 MongoDB behaviour is decided and documented (supported, degraded or rejected at compile time)
- [ ] #5 Works together with range, example, tenant and parent filters through FullTextQueryExecutor
- [ ] #6 Tested on PostgreSQL and MongoDB; Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
