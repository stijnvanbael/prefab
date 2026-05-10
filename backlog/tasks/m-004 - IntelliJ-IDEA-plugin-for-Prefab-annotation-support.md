---
id: M-004
title: IntelliJ IDEA plugin for Prefab annotation support
status: To Do
assignee: []
created_date: '2026-05-08 16:37'
labels: []
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Build an IntelliJ IDEA plugin that understands Prefab annotations. The plugin should provide annotation-aware inspections (flagging invalid combinations), navigation to generated artefacts, and endpoint preview hints — reducing the cognitive overhead of working with annotation-driven code generation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An IntelliJ IDEA plugin is published that understands Prefab annotations
- [ ] #2 The plugin provides inspections that flag invalid annotation combinations with quickfix suggestions
- [ ] #3 The plugin provides a 'Go to Generated Artefact' navigation action from an @Aggregate record
- [ ] #4 The plugin highlights which endpoints will be generated (method + path) as an inlay hint or gutter icon
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
