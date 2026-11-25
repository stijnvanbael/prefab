---
id: TASK-391
title: Pluggable model profiles for the agent benchmark
status: Done
assignee: []
created_date: '2026-11-25 12:00'
labels:
  - agents
  - testing
  - productivity
dependencies:
  - TASK-343
references:
  - backlog/docs/benchmark.md
priority: high
type: feature
ordinal: 216000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Running the benchmark on Haiku exhausts the token budget quickly. The initial baseline for comparing documentation
setups (TASK-346, TASK-382) moves to a local Qwen coder served by Ollama, and any other model must stay a
configuration change. Haiku moves to a follow-up (TASK-389) for the brownfield tier and the tasks that differ.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A model profile (`benchmark/models/<name>.yml`) names the model, agent-only environment variables, extra CLI arguments, a timeout factor and a default parallelism; `--profile` selects it and `--model` still works alone
- [x] #2 Profiles ship for a local Qwen via Ollama, Haiku and Sonnet; `list` shows them
- [x] #3 Documented in `benchmark.md`, including the Ollama context-window and tool-calling requirements and that pass rates are not compared across models
- [x] #4 Unit tests cover loading, defaults, validation and overriding the model
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- The agent stays Claude Code. Ollama exposes an Anthropic-compatible endpoint, so a profile only sets
  `ANTHROPIC_BASE_URL`, a placeholder token and the model; no new `Agent` implementation was needed. The profile
  environment applies to the agent process only, not to Maven or acceptance runs.
- `ClaudeCodeAgent` now takes a `ModelProfile`. `--resume` rebuilds the profile from the previous run's model unless
  `--profile` is given again, so a run on a non-default endpoint must name its profile when resumed.
- The Ollama endpoint settings in `qwen-local.yml` follow Ollama's Claude Code integration as documented; they have
  not been exercised against a live Ollama server in this environment. The `model` tag is a placeholder for the
  Qwen coder the owner pulls locally, and the first run should be a single task to confirm the endpoint.
- Not changed: `Triage` (`--triage-model`) still calls the hosted `claude` CLI.
<!-- SECTION:NOTES:END -->
