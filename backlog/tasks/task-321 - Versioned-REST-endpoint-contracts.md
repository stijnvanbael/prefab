---
id: TASK-321
title: Support independently versioned REST endpoint contracts
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [rest, feature]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Allow old and new REST contracts to coexist while sharing domain behavior, rather than duplicating aggregates for each API version. Investigate existing custom paths/adapters first and select one explicit default versioning strategy.

Related: TASK-179 (compatibility policy) and TASK-127 (exposure strategies). TASK-301 versions events, not HTTP endpoints.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Document the selected path/header/media-type strategy, opt-in metadata and version ownership; unversioned applications keep current mappings.
- [ ] #2 Demonstrate two versions with different request/response DTOs mapping to the same domain operation, with explicit validation and compatibility behavior.
- [ ] #3 Cover generated controllers, service adapters, test clients, OpenAPI identities, security and nested/polymorphic contracts consistently.
- [ ] #4 Mapping/version conflicts produce actionable compiler errors; tests verify both versions, unsupported versions and deprecation signaling.
- [ ] #5 Document migration/retirement guidance without implying domain or event versions automatically equal API versions.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Evaluate current path/custom-adapter capabilities, specify the smallest coexistence contract, implement two versioned endpoints and test generated artifacts and compatibility.
<!-- SECTION:PLAN:END -->
