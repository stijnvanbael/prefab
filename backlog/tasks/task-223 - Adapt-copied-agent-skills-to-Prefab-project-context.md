---
id: TASK-223
title: Adapt copied agent skills to Prefab project context
status: In Progress
assignee:
  - github-copilot
created_date: '2026-05-21 06:16'
updated_date: '2026-05-21 06:16'
labels:
  - agents
  - documentation
  - dx
dependencies: []
references:
  - /Users/stijnvanbael/IdeaProjects/appify/prefab/.agents/skills
  - /Users/stijnvanbael/IdeaProjects/appify/prefab/AGENTS.md
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/.github/copilot-instructions.md
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/docs/developer-guide.md
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The `.agents/skills/*/SKILL.md` files were copied from another project and still contain mostly generic role guidance. Update them so each skill reflects Prefab’s actual repository conventions: Java 25 + Maven, annotation processing, model-first development with `@Aggregate`, generated artefacts, extension points, backlog workflow, documentation obligations, example modules, and appropriate role boundaries for this codebase.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each skill under `.agents/skills/` reflects Prefab-specific responsibilities, workflow, and guardrails instead of generic project guidance.
- [ ] #2 Role descriptions reference actual repository concepts where relevant, including backlog workflow, Prefab generation boundaries, modules, developer guide, and example modules.
- [ ] #3 Low-fit roles are adapted so they remain useful in this repository without implying unsupported responsibilities.
- [ ] #4 All modified skill files are internally consistent in tone and structure and do not contradict `AGENTS.md` or `.github/copilot-instructions.md`.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Audit all existing `.agents/skills/*/SKILL.md` files against `AGENTS.md`, `.github/copilot-instructions.md`, and the Prefab developer guide.
2. Define a consistent structure for every skill with Prefab-specific scope, workflow, guardrails, preferred sources, and escalation paths.
3. Rewrite each skill so it references actual repository concerns such as model-first generation, modules, example projects, backlog discipline, and documentation obligations.
4. Review the full set for consistency and gaps, then run file validation and inspect the diff before wrapping up.
<!-- SECTION:PLAN:END -->
