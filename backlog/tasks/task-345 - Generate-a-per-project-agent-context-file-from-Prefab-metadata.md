---
id: TASK-345
title: Generate a per-project agent context file from Prefab metadata
status: To Do
assignee: []
created_date: '2026-11-20 20:13'
labels:
  - agents
  - annotation-processor
  - dx
dependencies: []
priority: high
type: feature
ordinal: 190000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generic documentation describes every Prefab feature. A given project only uses some of them: Postgres or MongoDB, Kafka, Pub/Sub or SNS/SQS, with or without Spring Security, and so on. An AI agent that reads generic docs wastes context on features that don't apply and can pick the wrong variant (for example a Mongo pattern in a Postgres app).

To see what Prefab produced for the current model, an agent currently has to dig through target/generated-sources. That is large, noisy, and tempts the model to edit generated files.

At compile time the annotation processor already knows the classpath (it decides which plugins are active from it) and the whole domain model. It can write a compact, accurate summary for agents: which features apply to this project, and what was generated (endpoints, events and topics, tables or collections, generated test clients and helpers, required configuration properties). TASK-185 (Mermaid diagram) uses the same metadata.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Compiling a Prefab project writes a single Markdown agent-context file to a documented, stable location under target/
- [ ] #2 The file lists which Prefab capabilities are active for this project based on its classpath, and omits the ones that are not
- [ ] #3 The file summarises the generated surface per aggregate: HTTP endpoints (method and path), events published and handled with their topics, persistence tables or collections, and generated test helpers
- [ ] #4 The file lists the configuration properties the application needs at runtime for the active modules
- [ ] #5 Output is deterministic: compiling the same model twice gives identical files, and it stays compact (documented size budget, verified by a test on a representative project)
- [ ] #6 Generation can be turned off with a processor option and is covered by annotation-processor tests
- [ ] #7 The file and how agents should use it are documented in the developer guide and referenced from the agent cheat sheet
<!-- AC:END -->
