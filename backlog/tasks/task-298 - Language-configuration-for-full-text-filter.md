---
id: TASK-298
title: 'Language configuration for the FULL_TEXT filter'
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
TASK-030 uses the language-neutral PostgreSQL `simple` configuration (no stemming, no stop words) and MongoDB's default language. Make this configurable so that stemming and stop words apply: per filter (attribute such as `language = "english"`) and as an application-wide default property. The executor already receives the configuration as a constructor argument, and the generated GIN index must use the same configuration.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Application-wide default text search configuration via a Spring property
- [ ] #2 Per-filter override on @Filter
- [ ] #3 PostgreSQL: configuration used consistently in the query and in the generated index expression; an unknown configuration is a compile error or fails fast at startup
- [ ] #4 MongoDB: default_language / language_override applied to the generated text index and the $text query
- [ ] #5 Changing the language results in a migration that rebuilds the index
- [ ] #6 Tested on PostgreSQL and MongoDB; Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
