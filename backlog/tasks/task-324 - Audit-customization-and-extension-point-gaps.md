---
id: TASK-324
title: Spike customization coverage and identify missing extension points
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [spike, architecture, dx]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Determine which Prefab opinions can be customized through supported declarative metadata, generation plugins, repository mixins and runtime providers, and identify concrete gaps that currently require forks or replacing whole generated classes. Aim for useful safe customization, not an unbounded promise that every internal detail is public API.

Related: TASK-180 (provider SPIs), TASK-227 (plugin configuration), TASK-127 (exposure modules), completed TASK-181 (escape-hatch docs) and completed TASK-284 (security hook). Reuse their contracts rather than proposing duplicates.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Produce a coverage matrix spanning generated contracts/controllers/services/repositories, persistence/migrations, messaging/serialization, security, tests, observability and infrastructure, linked to actual extension APIs.
- [ ] #2 Identify at least five realistic customization scenarios and classify each as declarative, supported hook, whole-class override, missing capability or intentionally protected invariant.
- [ ] #3 Prototype the highest-value missing hook and verify deterministic ordering, no-op default behavior, conflict handling and regeneration safety.
- [ ] #4 Record an ADR distinguishing stable extension contracts from internals, with compatibility/discovery/testing requirements and explicit limits.
- [ ] #5 Create prioritized, bounded implementation stories for genuine gaps after duplicate checks, and update the extension guide with current capabilities and supported escape hatches.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Inventory current hooks/configuration, test representative advanced customizations, select one gap for a bounded prototype and propose only evidence-backed API additions.
<!-- SECTION:PLAN:END -->
