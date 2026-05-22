---
id: TASK-223
title: Adapt copied agent skills to Prefab project context
status: Done
assignee:
  - github-copilot
created_date: '2026-05-21 06:16'
updated_date: '2026-05-21 06:24'
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
- [x] #1 Each skill under `.agents/skills/` reflects Prefab-specific responsibilities, workflow, and guardrails instead of generic project guidance.
- [x] #2 Role descriptions reference actual repository concepts where relevant, including backlog workflow, Prefab generation boundaries, modules, developer guide, and example modules.
- [x] #3 Low-fit roles are adapted so they remain useful in this repository without implying unsupported responsibilities.
- [x] #4 All modified skill files are internally consistent in tone and structure and do not contradict `AGENTS.md` or `.github/copilot-instructions.md`.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Audit all existing `.agents/skills/*/SKILL.md` files against `AGENTS.md`, `.github/copilot-instructions.md`, and the Prefab developer guide.
2. Define a consistent structure for every skill with Prefab-specific scope, workflow, guardrails, preferred sources, and escalation paths.
3. Rewrite each skill so it references actual repository concerns such as model-first generation, modules, example projects, backlog discipline, and documentation obligations.
4. Review the full set for consistency and gaps, then run file validation and inspect the diff before wrapping up.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Rewrote every `SKILL.md` under `.agents/skills/` to align the role guidance with Prefab's model-first workflow, generated artefacts, module structure, backlog discipline, and documentation rules.
- Added a shared structure across the skills with repository-specific scope, preferred sources, guardrails, workflow steps, and escalation paths.
- Repurposed low-fit roles such as `frontend-engineer`, `product-manager`, and `devops-engineer` so they remain useful in this framework repository without implying unsupported product-app responsibilities.
- Validated the markdown changes with file error checks, a whitespace diff check, and targeted shell checks for the shared section structure.
- Created conventional commits for the skill adaptation and for restoring an unrelated staged `todos.txt` deletion that was accidentally swept into the first commit.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Adapted the copied role skills to Prefab's repository reality.

Highlights:
- Replaced generic project guidance with Prefab-specific responsibilities centered on aggregates, annotations, generated artefacts, modules, examples, and backlog-driven documentation maintenance.
- Added consistent repository-aware sections such as preferred sources, guardrails, workflow, and escalation paths across all skills.
- Converted low-fit roles into useful framework-facing roles, especially around API consumer experience, roadmap shaping, and CI/release operations for a multi-module Maven codebase.
- Verified the changes with markdown/file validation and committed the result using conventional commits.
<!-- SECTION:FINAL_SUMMARY:END -->
