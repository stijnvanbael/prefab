---
id: TASK-322
title: Add fine-grained row and field authorization with custom security expressions
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [security, feature]
dependencies: []
priority: high
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Extend security beyond endpoint roles/authorities to per-record access, policy-dependent field visibility/writeability and developer-supplied expressions. Specify and deliver thin slices rather than a general policy language.

Related: completed TASK-284 (web-security customizers and roles), completed TASK-106 (tenant isolation), TASK-042 (unconditional hidden fields) and TASK-046 (examples). None provides principal-dependent row/field authorization. Decide explicitly whether database-native RLS is supported; do not equate application predicates with database RLS.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Specify opt-in policies, deny defaults and composition with tenant/endpoint security; policy failure never grants access.
- [ ] #2 Row restrictions apply before filtering/paging/counting on reads and before mutation on writes; cover get/list/search/autocomplete and protected operations without post-filter count leaks.
- [ ] #3 Field policies define omit/redact behavior for responses and reject/ignore behavior for unauthorized input explicitly; nested/polymorphic contracts and mass-assignment risks are covered.
- [ ] #4 Developer-authored Spring Security expressions/custom beans have explicit precedence versus role/authority settings, documented evaluation context and clear invalid-configuration diagnostics; runtime input cannot become expression source.
- [ ] #5 Tests cover allowed/denied principals, cross-tenant access, aggregates containing mixed-visibility fields and generated test-client behavior; document non-HTTP consumers and backend limitations.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Define policy contracts first; slice delivery into custom endpoint expressions, query/write row policies and field mapping/input policies. Reuse Spring Security and existing tenant/query hooks, adding separate child stories before implementation if needed.
<!-- SECTION:PLAN:END -->
