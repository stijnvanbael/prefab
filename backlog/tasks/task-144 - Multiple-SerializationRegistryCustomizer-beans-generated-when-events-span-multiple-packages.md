---
id: TASK-144
title: >-
  Multiple SerializationRegistryCustomizer beans generated when events span
  multiple packages
status: To Do
assignee: []
created_date: '2026-04-27 14:10'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When events are defined in multiple packages, the annotation processor generates a SerializationRegistryConfiguration per package. This results in multiple SerializationRegistryCustomizer beans conflicting with one another at runtime.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Only one SerializationRegistryConfiguration is generated regardless of how many packages events are spread across
- [ ] #2 All topics from all packages are registered in the single customizer
- [ ] #3 Existing single-package behaviour is unaffected
<!-- AC:END -->
