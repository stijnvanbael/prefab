---
id: task-072
title: Delete parent cascades to children
status: Done
assignee: []
created_date: '2026-01-06 07:28'
updated_date: '2026-01-08 17:46'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 24000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Cascading deletes in DDD over multiple aggregate roots is not a good idea. Alternate strategy: remove referential integrety contraints on references to aggregate roots and clean up orphans through event handling. Allow `@Delete` on a method to allow publishing an event.

We'll reintroduce optional constraints later if there is a use case for it.
<!-- SECTION:DESCRIPTION:END -->
