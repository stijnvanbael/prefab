---
id: TASK-345
title: Generate a per-project agent context file from Prefab metadata
status: Done
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
- [x] #1 Compiling a Prefab project writes a single Markdown agent-context file to a documented, stable location under target/
- [x] #2 The file lists which Prefab capabilities are active for this project based on its classpath, and omits the ones that are not
- [x] #3 The file summarises the generated surface per aggregate: HTTP endpoints (method and path), events published and handled with their topics, persistence tables or collections, and generated test helpers
- [x] #4 The file lists the configuration properties the application needs at runtime for the active modules
- [x] #5 Output is deterministic: compiling the same model twice gives identical files, and it stays compact (documented size budget, verified by a test on a representative project)
- [x] #6 Generation can be turned off with a processor option and is covered by annotation-processor tests
- [x] #7 The file and how agents should use it are documented in the developer guide and referenced from the agent cheat sheet
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- New `AgentContextPlugin` + `AgentContextRenderer` in `annotation-processor` (`processor/agent`), registered in the plugin `META-INF/services` file. It runs from `writeGlobalFiles`, so it sees every resolved aggregate once, after deferred rounds.
- Output: `target/prefab/agent-context.md` (test compilation: `agent-context-test.md`), derived from the CLASS_OUTPUT location so it is not packaged in the jar. Falls back to a `prefab/agent-context.md` CLASS_OUTPUT resource when the output is not a file-system directory (e.g. in-memory compilers).
- Capabilities come from `Elements.getTypeElement` probes of marker classes on the compile classpath; absent ones are omitted. Required properties are the standard Spring properties of the active capabilities.
- Opt-out reuses the existing plugin switch: `-Aprefab.plugin.agentcontext.enabled=false`.
- Published events are not attributed per aggregate (publishing happens in method bodies, not in metadata); they are listed in the project-wide Events section. Handled events are listed per aggregate.
- Size budget (2.5 KB + 0.7 KB per aggregate) is enforced in `AgentContextPluginTest`; the showcase example produces ~4.6 KB.
- AC #7: documented in `generated-artefacts.md` §6.12 and the developer guide index. The agent cheat sheet (TASK-344) does not exist yet, so TASK-344 now carries a note to reference this file.
<!-- SECTION:NOTES:END -->
