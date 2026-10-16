---
id: TASK-299
title: 'Prefix and type-ahead matching for the FULL_TEXT filter'
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
`websearch_to_tsquery` (PostgreSQL) and `` (MongoDB) only match whole words, so a user typing "spri" does not find "Spring". Add an opt-in prefix mode so the last term of the query matches as a prefix (PostgreSQL: `to_tsquery` with `:*` built from sanitised terms, never from raw input; MongoDB: anchored regex fallback). Complements, and must not duplicate, `@Autocomplete`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Opt-in prefix mode on @Filter; whole-word matching remains the default
- [ ] #2 The last term of the query is matched as prefix, earlier terms as whole words
- [ ] #3 User input is sanitised so special characters cannot break or alter the tsquery
- [ ] #4 MongoDB behaviour is decided and documented
- [ ] #5 Works together with range, example, tenant and parent filters
- [ ] #6 Tested on PostgreSQL and MongoDB; Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
