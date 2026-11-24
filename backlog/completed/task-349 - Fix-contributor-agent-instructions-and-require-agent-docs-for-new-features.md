---
id: TASK-349
title: Fix contributor agent instructions and require agent docs for new features
status: Done
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
- [x] #1 The living-document rule the developer guide refers to exists in the contributor instructions that AGENTS.md points to, and the reference resolves
- [x] #2 Version and Java information in the developer guide matches the current project, or is removed so it cannot go stale
- [x] #3 The contributor instructions require that a change to Prefab's public behaviour also updates the affected agent cheat sheet entry and recipe, and adds or updates a benchmark task
- [x] #4 The project Definition of Done reflects the same requirement
- [x] #5 Contributor instructions (for people changing Prefab) and app-developer agent guidance (for people building apps with Prefab) are clearly separated so neither is loaded by mistake
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Analysis: AGENTS.md had already become the single instruction file (`.github/copilot-instructions.md` is a symlink to it), so the dangling reference was to a "living document rule" that no longer existed there.
- AGENTS.md: added a "Living document rule" section (reference docs, cheat sheet, recipe, benchmark task, generated agent context) plus a "Contributor versus app-developer guidance" section, and extended the pre-done checklist. Items for artefacts that do not exist yet (TASK-344/345/346) apply once they land on `main`; until then the follow-up is recorded in the backlog task.
- developer-guide.md: removed the stale "Version 0.7.x / Java 21+" header line; versions now point to the readme quick start. The living-document note links to the new AGENTS.md anchor.
- BACKLOG.md Definition of Done: documentation item now includes the cheat sheet, recipe and benchmark task for public behaviour changes.
- Separation: the contributor files are enumerated, app-developer material must not live in or be referenced from them. Where app-developer guidance is stored is decided by TASK-344/350.
<!-- SECTION:NOTES:END -->
