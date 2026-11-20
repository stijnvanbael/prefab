---
id: TASK-349
title: Fix contributor agent instructions and require agent docs for new features
status: To Do
assignee: []
created_date: '2026-11-20 20:14'
labels:
  - agents
  - documentation
dependencies: []
priority: medium
type: chore
ordinal: 194000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The contributor-facing agent instructions have drifted. backlog/docs/developer-guide.md says agents must follow a 'living document rule' defined in AGENTS.md, but AGENTS.md only points to .github/copilot-instructions.md, which contains no such rule. The developer guide header still says 'Version 0.7.x' and 'Java 21+' while the project is at 0.13 and targets Java 25.

As Prefab moves towards AI-driven development, agent-facing material (cheat sheet, recipes, generated agent context, benchmark tasks) must stay in step with every feature change, or small models will copy outdated guidance. That only happens if it is part of how contributors, human or agent, finish a feature.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The living-document rule the developer guide refers to exists in the contributor instructions that AGENTS.md points to, and the reference resolves
- [ ] #2 Version and Java information in the developer guide matches the current project, or is removed so it cannot go stale
- [ ] #3 The contributor instructions require that a change to Prefab's public behaviour also updates the affected agent cheat sheet entry and recipe, and adds or updates a benchmark task
- [ ] #4 The project Definition of Done reflects the same requirement
- [ ] #5 Contributor instructions (for people changing Prefab) and app-developer agent guidance (for people building apps with Prefab) are clearly separated so neither is loaded by mistake
<!-- AC:END -->
