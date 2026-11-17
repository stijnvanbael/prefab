---
id: TASK-326
title: Add aggregate-bound frontend view model (@ViewOf)
status: To Do
assignee: []
created_date: '2026-11-17 10:00'
labels: [feature, rest, bff]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Let a record declare a frontend view of an aggregate (`@ViewOf(Channel.class)`) with a field subset, renames and `@Computed` values. Prefab generates the mapper, service method, REST endpoint, test client and mother, reusing generated paging, `@Filter`, tenant and security behaviour. Removes the handwritten DTO/mapping/controller boilerplate shown in the TASK-320 prototypes. Rationale: decision-005. Complements TASK-170 and TASK-319; coordinate with TASK-127 (exposure strategies) and TASK-321 (versioned contracts).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Prototype via a PrefabPlugin first; document which plugin hooks were insufficient
- [ ] #2 Unknown view fields or type mismatches against the aggregate produce actionable compiler errors
- [ ] #3 A view generates mapper, service method, GET-by-id and paged GET-list endpoints, test client and mother without handwritten code
- [ ] #4 Tenant filtering and security configuration of the aggregate apply to the view, including counts
- [ ] #5 Several views of one aggregate coexist; the aggregate record is unchanged
- [ ] #6 Documented in the developer guide and annotation reference; the Kafka example replaces the handwritten dashboard DTO where applicable
<!-- AC:END -->
