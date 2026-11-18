---
id: TASK-329
title: Evaluate generated aggregate-free application operations
status: To Do
assignee: []
created_date: '2026-11-18 10:37'
labels:
  - spike
  - architecture
  - dx
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Assess an opt-in generation contract for stateless operations such as tax calculation or a shipping-rate facade without requiring an aggregate, repository, or artificial persistent state. TASK-309 and decision-006 identified this as a strong candidate, not an adoption decision. Coordinate exposure with TASK-127 and compare with its planned service reuse; preserve existing DDD defaults.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Compare handwritten Spring and existing aggregate-based approaches against an isolated operation-only prototype for a concrete request/response workflow, including authored code and generated output.
- [ ] #2 Specify how validation, authorization, error mapping, tenant context, OpenAPI and test clients would work and which concerns remain developer-owned; show failure and invalid-input handling.
- [ ] #3 Show compile-time/runtime tests for the prototype, and verify that projects without the opt-in produce unchanged aggregate artifacts and routes.
- [ ] #4 Document ownership, extension and migration costs plus a go/no-go recommendation; propose implementation slices only if justified, without requiring TASK-127 to be completed first.
<!-- AC:END -->
