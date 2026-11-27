---
id: TASK-401
title: Investigate additional agent aids combined with recipes
status: To Do
assignee: []
created_date: '2026-11-27 09:00'
labels:
  - agents
  - testing
  - documentation
dependencies:
  - TASK-399
references:
  - backlog/decisions/decision-014 - Recipes-as-the-baseline-benchmark-context.md
  - backlog/docs/benchmark.md
  - backlog/completed/task-392 - Record-the-local-Qwen-baseline-across-documentation-contexts.md
  - backlog/tasks/task-350 - Deliver-version-matched-agent-guidance-to-Prefab-application-projects.md
  - backlog/tasks/task-127 - MCP-Model-Context-Protocol-support-and-optional-REST-module.md
priority: medium
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`recipes` is the baseline benchmark context (decision-014). Find out which additional aids, layered on top of recipes,
make agents faster, cheaper or more reliable when building Prefab applications, and adopt the ones that measurably
help.

Each candidate becomes a benchmark context that extends `recipes` and is measured against plain `recipes` on the same
model (local Qwen 3.8 27B first, Haiku later). On quick tasks, where every setup passes, compare agent time, turns,
builds and tokens; for pass rate, use the feature and brownfield tiers (TASK-389).

Candidates seen in TASK-392 and earlier runs:
- **Recipes + local run** (`local-run` instruction on top of recipes): did starting the app help or only cost time?
- **Behavioural guardrails in the preamble or cheat sheet**: do not delete a failing test; a missing method on the
  generated `{Aggregate}Client` means the endpoint was not generated; work with paths under the project root. Small
  models gave up on their own tests and wrote outside the workspace.
- **Reading `target/prefab/agent-context.md` first**, made more prominent or inlined after each compile.
- **On-demand reference docs** (annotation reference, troubleshooting) copied next to the recipes instead of loaded
  into CLAUDE.md: the full docs cost 3x the tokens when always loaded.
- **Tools**: a Prefab MCP server or skill that answers "how do I ..." and "what was generated" (TASK-127), and a
  scaffold command (TASK-378).
- **Better diagnostics** as an aid: wrong imports and misplaced annotations were read as missing features
  (TASK-398 is one instance).

Runs must be isolated (TASK-399) so results are not contaminated by reference solutions on the host.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A shortlist of candidate aids is agreed, each with the hypothesis it tests and the context that implements it
- [ ] #2 Each shortlisted aid is recorded as a context extending `recipes` and compared against plain `recipes` on the same model and tasks, with time, turns, builds, tokens and pass rate
- [ ] #3 Results and the decision per aid (adopt, drop, needs more data) are recorded in `benchmark.md`; adopted aids update the baseline context or the guidance shipped to applications (TASK-350)
- [ ] #4 Aids that help are confirmed with `--repeat 3` or on the feature tier before adoption
<!-- AC:END -->
