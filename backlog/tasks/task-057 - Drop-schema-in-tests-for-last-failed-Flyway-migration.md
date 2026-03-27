---
id: TASK-057
title: Drop schema in tests for last failed Flyway migration
status: In Progress
assignee: []
created_date: '2025-12-26 13:14'
updated_date: '2026-03-26 18:00'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
In order to have a constant development flow, in case there is a checksum mismatch for the last Flyway migration during tests, automatically drop the schema and try migrations again.
<!-- SECTION:DESCRIPTION:END -->
