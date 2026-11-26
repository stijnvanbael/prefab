---
id: TASK-397
title: Report energy cost instead of API cost for local model profiles
status: To Do
assignee: []
created_date: '2026-11-26 11:45'
labels:
  - agents
  - testing
dependencies: []
references:
  - backlog/docs/benchmark.md
  - benchmark/src/main/java/be/appify/prefab/benchmark/ClaudeCodeAgent.java
priority: low
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during the TASK-392 pilot: a `qwen-local` run reported `Cost $2,06` for one attempt. Claude Code reports
`total_cost_usd` from its own API price table even when the model is served by Ollama, and `ClaudeCodeAgent` takes
that value as is. That figure is meaningless for a local model. A local run is not free either, though: its cost is
the energy the host consumes while the model works.

A profile should be able to replace the API price with an energy estimate, for example average power draw while
serving (`watts`) and an electricity price (`pricePerKwh`), giving cost = watts x agent time / 1000 x price per kWh.
Measuring actual draw (for example `powermetrics` on macOS) is more precise but needs root; a configured estimate is
enough to compare setups on the same machine. Until a profile sets these, the report should show the cost as unknown,
not as an API price.

`benchmark.md` currently says cost columns read zero for a local model; correct that as well.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A local profile can declare its power draw and electricity price, and its runs report the estimated energy cost
- [ ] #2 A local profile without those fields reports cost as unknown, never Claude Code's API price
- [ ] #3 Hosted profiles keep reporting Claude Code's cost
- [ ] #4 `benchmark.md` documents the fields and how to estimate the power draw
<!-- AC:END -->
