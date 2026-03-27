---
id: TASK-095
title: Make test clients accept the fields of the request object
status: Done
assignee: []
created_date: '2026-03-25 18:02'
updated_date: '2026-03-26 11:05'
labels:
  - "\U0001F9F9chore"
dependencies: []
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Make test clients accept the fields of the request object instead of the request object itself. Construct the request object in the generated code. This way there is less overhead in tests.
<!-- SECTION:DESCRIPTION:END -->
