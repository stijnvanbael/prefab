---
id: task-052
title: >-
  Hardwire topic names into publishers and subscribers when the topic name is
  not an expression
status: Done
assignee: []
created_date: '2025-12-26 08:33'
updated_date: '2025-12-26 12:10'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 16000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Topic names without a Spring config expression result in invalid @Value annotations

- When the topic name is like "${...}" -> use @Value
- Else -> hardwire topic name
<!-- SECTION:DESCRIPTION:END -->
