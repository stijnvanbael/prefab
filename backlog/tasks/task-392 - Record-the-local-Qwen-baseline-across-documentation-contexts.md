---
id: TASK-392
title: Record the local Qwen baseline across documentation contexts
status: To Do
assignee: []
created_date: '2026-11-25 12:00'
labels:
  - agents
  - testing
  - documentation
dependencies:
  - TASK-391
references:
  - backlog/docs/benchmark.md
priority: high
type: task
ordinal: 217000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Record the initial benchmark baseline with the `qwen-local` profile (TASK-391) instead of Haiku, so documentation
setups can be compared without spending API tokens. This closes the model-run criteria of TASK-346 (#6, recipes) and
TASK-382 (#7, local-run). Compare setups on the same model; do not compare pass rates between models.

Run on the owner's machine (Ollama with a context window of at least 32k): `--profile qwen-local --repeat 3` with the
contexts `none`, `cheat-sheet`, `recipes`, `local-run` and `full-docs`, starting with a one-task pilot to confirm the
endpoint and tool calling work. Classify every failure and note which tasks differ between contexts; those feed the
Haiku follow-up in TASK-389.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A one-task pilot on `qwen-local` completes through the agent and acceptance steps, confirming endpoint, context window and tool calling
- [ ] #2 Runs for `none`, `cheat-sheet`, `recipes`, `local-run` and `full-docs` with `--repeat 3` are recorded in `benchmark/results/` and classified, with turns, builds and pass rate compared
- [ ] #3 `benchmark.md` records the Qwen baseline and lists the tasks whose outcome differs between contexts, and the TASK-346 and TASK-382 notes reference it
- [ ] #4 If Qwen fails nearly everything or passes nearly everything, the result says so and proposes another model profile instead of drawing conclusions
<!-- AC:END -->
