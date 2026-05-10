---
id: task-185
title: Generate Mermaid.js architecture diagram from Prefab metadata
status: To Do
assignee: []
created_date: '2026-05-08'
labels: []
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Because Prefab's annotations carry explicit structural metadata (`@Aggregate`, `@Event`,
`@EventHandler`, `@Create`, `@Update`, `@Delete`, `@GetById`, `@GetList`, `@Filter`), the annotation
processor has everything it needs to visualise a system's architecture automatically.

Add a `prefab-diagram` module (or extend the existing annotation processor) to generate a
[Mermaid.js](https://mermaid.js.org/) diagram file at compile time. The diagram should show:

- Each aggregate as a node with its exposed REST endpoints
- Each event as a node with its topic
- Directed edges from aggregates to the events they publish
- Directed edges from events to the aggregates that handle them (`@EventHandler`)
- Relationships between aggregates where one references another via `Reference<T>` fields

The output is a single `target/prefab-diagram.md` (or `.mmd`) file renderable by any Mermaid-aware
tool (IntelliJ, GitHub, VS Code, mkdocs). Seeing the full system map after writing only a handful of
annotated records is a powerful "wow" moment for new adopters and a useful architecture review
artifact for existing teams.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A Mermaid class/flowchart diagram is generated to `target/prefab-diagram.md` during `mvn compile`
- [ ] #2 Each @Aggregate appears as a node labelled with its name and lists its REST endpoints (method + path)
- [ ] #3 Each @Event appears as a node labelled with its name and topic
- [ ] #4 Publish relationships (aggregate → event) are shown as directed edges
- [ ] #5 Consume relationships (event → aggregate via @EventHandler) are shown as directed edges
- [ ] #6 Reference<T> fields between aggregates are shown as association edges
- [ ] #7 Diagram generation can be disabled via a processor option (e.g. `-Aprefab.diagram=false`)
- [ ] #8 The generated diagram renders correctly when pasted into the Mermaid Live Editor (https://mermaid.live)
- [ ] #9 Developer Guide documents the feature and shows an example rendered diagram
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->

