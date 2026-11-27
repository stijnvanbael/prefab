---
id: TASK-397
title: Estimate the cost of every benchmark run
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
  - benchmark/src/main/java/be/appify/prefab/benchmark/ModelProfile.java
priority: low
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found during TASK-392: a `qwen-local` attempt reported `Cost $2,06`. Claude Code reports `total_cost_usd` from its own
API price table even when the model is served by Ollama, and `ClaudeCodeAgent` takes that value as is, so the cost
column of a local run is meaningless and cannot be compared with hosted runs.

Report an **estimated cost** for every run instead, so setups and models can be compared on what they cost:
- **Hosted models:** the API cost Claude Code reports, as today.
- **Local models:** an estimate based on energy: average power draw while serving (`watts` in the profile) times
  agent time, times an electricity price (`pricePerKwh`). A configured draw is enough to compare setups on one
  machine; measuring it (for example `powermetrics` on macOS, which needs root) can refine it later.

The report and comparison say which way each cost was estimated. A profile without the fields reports the cost as
unknown, never Claude Code's API price for a model it did not serve.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Every report shows an estimated cost per attempt and per run, labelled as API-priced or energy-based
- [ ] #2 A local profile can declare its power draw and electricity price; its runs report the energy-based estimate
- [ ] #3 A local profile without those fields reports cost as unknown, never Claude Code's API price
- [ ] #4 Hosted profiles keep reporting Claude Code's cost
- [ ] #5 `benchmark.md` documents the fields and how to estimate the power draw, and the comparison table makes clear when costs are estimated differently
<!-- AC:END -->
