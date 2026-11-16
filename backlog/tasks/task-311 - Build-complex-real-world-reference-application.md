---
id: TASK-311
title: Build a complex real-world reference application with Prefab
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [example, feature]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Deliver a coherent, runnable reference application proving Prefab can support realistic business workflows rather than isolated annotation demos. Select and document a domain such as multi-tenant order fulfillment with orders, inventory, payments and customer-facing projections.

Related: TASK-043/TASK-046 (focused examples) and existing messaging/streams examples. Reuse shipped capabilities; track missing framework features separately rather than embedding private framework replacements.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Document the selected domain, aggregate boundaries and at least three complete workflows, including one failure/compensation path.
- [ ] #2 Demonstrate at least four collaborating aggregates, event-driven projections, persistence evolution, tenant-aware security, observability and a supported customization hook.
- [ ] #3 Provide deterministic seed data, local infrastructure instructions and executable end-to-end tests for successful, unauthorized and failed/retried operations.
- [ ] #4 Explain which code is generated versus handwritten, consistency/transaction trade-offs and why each escape hatch is needed.
- [ ] #5 Integrate the application into the examples build/CI and link it from the README and guide without presenting unfinished features as available.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Define a bounded domain and acceptance flows first, implement one vertical workflow at a time using existing modules, then add resilience, documentation and CI coverage.
<!-- SECTION:PLAN:END -->
