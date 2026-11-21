---
id: TASK-125
title: Provide a learning path for Agents to discover and understand Prefab
status: To Do
assignee: []
created_date: '2026-04-18 08:55'
updated_date: '2026-11-20 20:14'
labels:
  - documentation
  - agents
  - dx
dependencies: []
priority: medium
ordinal: 139000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Agents (AI coding assistants) need a clear, structured way to learn how Prefab works — its annotations, generated code, supported types, and extension points — so they can generate correct Prefab-based code without guessing. The primary discovery mechanism should be Javadoc on the public API, supplemented by a concise entry-point document that points agents to the right starting places (readme, modules, Javadoc).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All public Prefab annotations (@Aggregate, @Create, @Update, @Delete, @GetById, @GetList, @Event, @EventHandler, etc.) carry Javadoc that explains purpose, attributes, and at least one usage example
- [ ] #2 All public Prefab types (Reference, Binary, AuditInfo, etc.) carry Javadoc that explains purpose and usage
- [ ] #3 Package-level package-info.java files exist for every public-API package, giving agents a navigable entry point into each module
- [ ] #4 A dedicated AGENTS.md section (or separate doc under backlog/docs/) describes the recommended learning path: readme → Javadoc entry points → example modules
- [ ] #5 The annotation-processor module exposes Javadoc for its public SPI so agents can understand how to write custom plugins
<!-- AC:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-11-20 20:14
---
Related agent-readiness work: TASK-344 (cheat sheet), TASK-346 (task recipes) and TASK-350 (deliver version-matched guidance to application projects). Javadoc and package-info from this task remain the in-jar fallback that agents can read from sources jars.
---
<!-- COMMENTS:END -->
