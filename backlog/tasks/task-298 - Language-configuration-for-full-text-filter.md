---
id: TASK-298
title: 'Language configuration for the FULL_TEXT filter'
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
TASK-030 uses the language-neutral PostgreSQL `simple` configuration (no stemming, no stop words) and MongoDB's default language. Make this configurable so that stemming and stop words apply: per filter (attribute such as `language = "english"`) and as an application-wide default property. The executor already receives the configuration as a constructor argument, and the generated GIN index must use the same configuration.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Application-wide default text search configuration via a Spring property
- [x] #2 Per-filter override on @Filter
- [x] #3 PostgreSQL: configuration used consistently in the query and in the generated index expression; an unknown configuration is a compile error or fails fast at startup
- [x] #4 MongoDB: default_language / language_override applied to the generated text index and the $text query
- [x] #5 Changing the language results in a migration that rebuilds the index
- [x] #6 Tested on PostgreSQL and MongoDB; Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

- `@Filter.language` (default empty) and Spring property `prefab.full-text.language` (default `simple`); `FullTextCondition.language` carries the filter language, executors fall back to the property default.
- PostgreSQL: query and GIN index share the configuration; index name gets a `_<language>` suffix (none for `simple`) so a language change yields DROP + CREATE in the next migration. Default for the index is the compiler option `-Aprefab.fullText.language` (must equal the Spring property). Unknown language = compile error; unknown default property fails at startup (`pg_ts_config` check).
- MongoDB: generated `MongoIndexConfiguration` uses `MongoTextIndexes.createOrRebuild` (drops a text index with another default language), `` set on the `` query; `simple` maps to `none`.
- Tests: Postgres (Testcontainers) + Mongo (mock) executors, processor migration/compile-error/index tests. Docs: annotation-reference.md.