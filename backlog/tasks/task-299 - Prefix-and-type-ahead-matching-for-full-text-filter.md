---
id: TASK-299
title: 'Prefix and type-ahead matching for the FULL_TEXT filter'
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
`websearch_to_tsquery` (PostgreSQL) and `` (MongoDB) only match whole words, so a user typing "spri" does not find "Spring". Type-ahead is the default (changed from opt-in): the last term of the query matches as a prefix (PostgreSQL: `to_tsquery` with `:*` built from sanitised terms, never from raw input; MongoDB: anchored regex fallback). `@Autocomplete` gets the same capability through `MatchStrategy.FULL_TEXT`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Type-ahead is the default on @Filter (`typeahead = true`); `typeahead = false` opts out to whole-word matching
- [x] #2 The last term of the query is matched as prefix, earlier terms as whole words
- [x] #3 User input is sanitised so special characters cannot break or alter the tsquery
- [x] #4 MongoDB behaviour is decided and documented
- [x] #5 Works together with range, example, tenant and parent filters
- [x] #6 Tested on PostgreSQL and MongoDB; Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- `Filter.typeahead()` defaults to true; generated code appends `.withTypeahead()` to the `FullTextCondition`. Fields sharing a parameter must agree (compile error otherwise).
- `TypeaheadQuery` (core) splits the text into whole words and a letters/digits-only prefix; no prefix for trailing whitespace/punctuation, `-word`, open quote or `OR`.
- PostgreSQL: `websearch_to_tsquery(rest) && to_tsquery(prefix:*)` with bound parameters; also used for `ts_rank_cd`.
- MongoDB (decision): `` for the earlier words, anchored `\b` case-insensitive regex `` over the searched properties for the last word; single-word queries have no relevance score.
- `@Autocomplete`: new `MatchStrategy.FULL_TEXT` (PostgreSQL tsvector/tsquery on a generated GIN index, MongoDB word-boundary regex fallback). Docs updated in annotation-reference.md and feature-guides.md.
<!-- SECTION:NOTES:END -->

- Follow-up: plain search text now matches every word as prefix (any order) and a single word as initials ("SBG" finds "Spring Boot guide"); web-search texts keep last-word-only prefix. Initials use a generated btree index on a lower/regexp_replace expression (`InitialsExpression`). Fuzzy is per word with a prefix alternative. `@Autocomplete` has the same behaviour via `FULL_TEXT` and the new `FULL_TEXT_FUZZY`; MongoDB uses `TypeaheadQuery.valueRegex` built in the service.
