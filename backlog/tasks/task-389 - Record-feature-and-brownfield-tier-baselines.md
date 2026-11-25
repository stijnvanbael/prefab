---
id: TASK-389
title: Record feature and brownfield tier baselines
status: To Do
assignee: []
created_date: '2026-11-25 07:00'
labels:
  - agents
  - testing
  - productivity
dependencies:
  - TASK-377
references:
  - backlog/docs/benchmark.md
priority: high
type: task
ordinal: 214000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-377 added 8 feature and 6 brownfield benchmark tasks and validated them with the stand-in agents, but could not
record model runs: the cloud session it was built in may not start nested Claude Code sessions. Without baselines we
do not know whether the new tiers separate the documentation setups the way the saturated quick tier no longer does.

Record, with an API key and Docker, `--tasks feature,brownfield --repeat 3`:

- Haiku 5.5 with `full-docs` and with `docs-folder` (and `cheat-sheet` / `recipes` if budget allows);
- Sonnet 5.5 with `full-docs` as the ceiling.

Classify every failure, add the runs and a comparison to `benchmark/results/`, and replace "Feature and brownfield
tiers: not yet recorded" in `backlog/docs/benchmark.md` with the results. If there is no measurable gap between Haiku
and Sonnet on the new tiers, record why and propose harder tasks.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Haiku (full docs, docs on demand) and Sonnet runs of the feature and brownfield tiers with --repeat 3 are recorded and classified
- [ ] #2 benchmark.md reports the per-tier results and whether the tiers show a measurable gap between the setups
<!-- AC:END -->
