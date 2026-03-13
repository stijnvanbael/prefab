---
id: TASK-093
title: 'Spring Data JDBC: child entities always get deleted and inserted again'
status: To Do
assignee: []
created_date: '2026-03-13 14:51'
updated_date: '2026-03-13 14:52'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 227.81133651733398
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Spring Data JDBC always deletes and inserts all children of an aggregate root on each change to the aggregate root. This has a severe impact on performance and should be avoided.
<!-- SECTION:DESCRIPTION:END -->
