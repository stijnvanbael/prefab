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
  - TASK-392
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

The initial baseline now runs on a local Qwen (TASK-391, TASK-392) to save tokens. This task is the hosted-model
follow-up and stays small on purpose. Record, with an API key and Docker, `--repeat 3`:

- Haiku 5.5 (`--profile haiku`) on the `brownfield` tier, with `full-docs` and `docs-folder`;
- Haiku 5.5 on the other feature and quick tasks only where the Qwen baseline (TASK-392) showed a difference between
  contexts;
- optionally Sonnet 5.5 with `full-docs` as the ceiling.

Classify every failure, add the runs and a comparison to `benchmark/results/`, and replace "Feature and brownfield
tiers: not yet recorded" in `backlog/docs/benchmark.md` with the results. If there is no measurable gap between Haiku
and Sonnet on the new tiers, record why and propose harder tasks.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Haiku (full docs, docs on demand) runs of the brownfield tier and of the tasks that differed in the Qwen baseline, with --repeat 3, are recorded and classified; a Sonnet ceiling run is optional
- [ ] #2 benchmark.md reports the per-tier results and whether the tiers show a measurable gap between the setups
<!-- AC:END -->
