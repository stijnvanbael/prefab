---
id: TASK-090
title: 'Spring Data JDBC: @PersistenceCreator constructor is required'
status: Done
assignee: []
created_date: '2026-02-07 19:41'
updated_date: '2026-03-18 18:58'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 10000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Spring Data JDBC requires you to manually define a @PersistenceCreator annotated constructor to reconstruct your aggregate roots from the DB. Find a way around it to make the all argument constructor the default for Spring Data JDBC.
<!-- SECTION:DESCRIPTION:END -->
