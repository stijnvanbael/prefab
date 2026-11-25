---
id: TASK-377
title: Add feature-sized and brownfield tasks to the agent benchmark
status: In Progress
assignee: []
created_date: '2026-11-24 10:00'
labels:
  - agents
  - testing
  - productivity
dependencies:
  - TASK-343
priority: high
type: task
ordinal: 206000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The TASK-343 benchmark is saturated. Haiku passes 29-31 of 32 tasks in every documentation setup and Sonnet 32/32; all
differences are inside the confidence intervals and the run-to-run margin is two tasks. It can no longer tell whether
the cheat sheet (TASK-344), recipes (TASK-346), local runs (TASK-382) or conformance tests (TASK-383) make agents better.
Every task is one feature on a tiny greenfield starter, done in about a minute.

Real work looks different: a feature spanning several aggregates and events from a product-style spec, a change to an
existing app the agent did not write, evolving a schema that has data in it, fixing a failing build or test. That is
where small models drift, fall back to plain Spring or work around generated code, and where Prefab's guidance pays off.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 At least 8 feature-sized tasks (several aggregates, events and handlers, security or tenancy combined), each written as a user story with acceptance criteria rather than Prefab vocabulary
- [ ] #2 At least 6 brownfield tasks start from a copy of the showcase (TASK-337) or another non-trivial app: add a feature, change a contract with backward compatibility, evolve a schema with existing data, fix a failing test, find and fix a bug from a report
- [ ] #3 Tasks are tagged by tier so `run` and `compare` report per tier; the existing 32 tasks stay as the quick tier
- [ ] #4 The report adds time-to-first-green-build and the number of generated classes overridden or hand-written alternatives, alongside pass rate
- [ ] #5 Baselines for Haiku (full docs, docs on demand) and a stronger model show a measurable gap on the new tiers, or the task records why there is none
- [ ] #6 A documented, opt-in way to run the quick tier on a schedule (for example a manually triggered GitHub workflow with a repository secret) tracks regressions between releases
- [ ] #7 `benchmark.md` describes the tiers and how to add a brownfield task
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Analysis (2026-11-25):

- **Tiers.** `task.yml` gets `tier: quick | feature | brownfield` (default `quick`, so the existing 32 tasks need no
  change). Every `TaskResult` records its tier (old `report.json` files read as `quick`). `--tasks` also matches tier
  names, so `--tasks quick` runs the quick tier. `report.md` gets a "By tier" table and `compare` a pass-rate row per
  tier.
- **Brownfield base.** Brownfield tasks name a base application (`base: shop`) from `benchmark/apps/<name>/`
  (`app.yml` with description and modules, `project/` overlaid on the template before the starter). The `shop` app is
  a snapshot of the showcase core (catalog, customers, ordering, invoicing, inventory; Postgres + Kafka + security),
  renamed to `com.example.shop`, without the Avro/Streams slices, the capability test framework and the hand-written
  order-history controller. A snapshot instead of a live copy keeps runs comparable over time.
- **Metrics.** Claude Code's stream-json has no timestamps, so `ClaudeCodeAgent` records the arrival time of every
  transcript line in `transcript.times`; the parser takes the first tool result of a Maven Bash call that is not an
  error and shows no build or test failure as the first green build. Overrides are counted from guardrail violations
  (`generated-class-override`, hand-written controller, repository, persistence, consumer, JPA), as distinct files.
- **Schedule.** A `workflow_dispatch` GitHub workflow runs the quick tier with the `ANTHROPIC_API_KEY` secret; a weekly
  schedule only runs when the repository variable `BENCHMARK_SCHEDULE` is `true`.
- **Baselines.** Running `claude` from inside this cloud session is denied by the session's permission policy, so
  baselines for the new tiers have to be recorded from a machine (or the workflow) with an API key.
<!-- SECTION:PLAN:END -->
