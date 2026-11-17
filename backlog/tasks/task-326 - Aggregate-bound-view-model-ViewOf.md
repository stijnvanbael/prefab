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
Code-first, minimal-annotation design (see decision-005): one annotation binds a view record to an aggregate; the developer writes mapping in plain Java via `ViewAssembler` (batch read: joins, aggregation, enrichment) and `ViewCommands` (create/update/delete delegating to generated services). A default mapper is generated when view fields equal aggregate fields. Prefab generates controller, paging, tenant/security scoping, validation, transaction boundary, test client and mother. No string-based DSL. Removes the handwritten DTO/mapping/controller boilerplate shown in the TASK-320 prototypes. Rationale: decision-005. Complements TASK-170 and TASK-319; coordinate with TASK-127 (exposure strategies) and TASK-321 (versioned contracts).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Prototype via a PrefabPlugin first; document which plugin hooks were insufficient
- [ ] #2 Unknown view fields or type mismatches against the aggregate produce actionable compiler errors
- [ ] #3 A view generates mapper (default or via `ViewAssembler`), GET-by-id and paged GET-list endpoints, test client and mother
- [ ] #3b `ViewCommands` create/update/delete endpoints are generated only for implemented operations, with validation and a transaction boundary
- [ ] #3c Source rows are tenant/security scoped before the assembler runs; assemblers receive a batch, never a single item
- [ ] #4 Tenant filtering and security configuration of the aggregate apply to the view, including counts
- [ ] #5 Several views of one aggregate coexist; the aggregate record is unchanged
- [ ] #6 Documented in the developer guide and annotation reference; the Kafka example replaces the handwritten dashboard DTO where applicable
<!-- AC:END -->
