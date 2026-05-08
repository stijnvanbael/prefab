---
id: task-171
title: Document annotation compatibility matrix in Developer Guide
status: To Do
assignee: []
created_date: '2026-05-08 16:37'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a dedicated section to the Developer Guide listing all annotation combinations, which are valid, which produce compile-time errors, and which are silently ignored. This reduces onboarding friction and prevents misuse without changing any behavior.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Developer Guide contains a compatibility table covering all Prefab annotations
- [ ] #2 Each invalid combination documents the compile-time error message produced
- [ ] #3 Table is kept up to date by the living-document rule in AGENTS.md
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
