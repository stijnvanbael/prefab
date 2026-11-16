---
id: TASK-308
title: Showcase Prefab capabilities and complex customization in the README
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [documentation, dx]
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Make `readme.md` a compelling, accurate entry point for adopters. Link the Developer Guide near the top, describe every area Prefab currently touches, and demonstrate how meaningful real-world customization fits its declarative model and supported escape hatches. Keep the quick start short; put depth in the guide rather than duplicating it.

Related: TASK-148 (authoritative guide), TASK-125 (agent learning path), TASK-171 (annotation compatibility). This story concerns README discovery and positioning, not implementing roadmap features.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A prominent Developer Guide link appears immediately below the title/intro, before the quick start.
- [ ] #2 A capability inventory checked against modules, annotations, examples and existing guide pages covers domain modeling, REST and generated contracts, persistence/migrations/indexing, messaging/schema evolution, streams, security/tenancy/audit, validation/binary storage/search, test helpers, observability, infrastructure generation and extension points.
- [ ] #3 Every capability has a working deep link to the relevant guide section; unsupported or planned capabilities are clearly distinguished from released behavior.
- [ ] #4 At least three executable or source-linked complex scenarios show combinations of features and customization, including repository queries, plugin generation and runtime provider/security customization without editing generated sources.
- [ ] #5 Unsupported blanket claims about application complexity are replaced with evidence-based boundaries and trade-offs; setup/version/module examples remain accurate and links/snippets are checked.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Inventory shipped features and public customization hooks, select existing compilable examples, reorganize the README around discovery and use cases, and link missing depth to guide work rather than advertising unimplemented features.
<!-- SECTION:PLAN:END -->
