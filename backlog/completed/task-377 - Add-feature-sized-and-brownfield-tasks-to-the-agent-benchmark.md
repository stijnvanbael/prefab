---
id: TASK-377
title: Add feature-sized and brownfield tasks to the agent benchmark
status: Done
assignee: []
created_date: '2026-11-24 10:00'
labels:
  - agents
  - testing
  - productivity
updated_date: '2026-11-25 07:00'
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
- [x] #1 At least 8 feature-sized tasks (several aggregates, events and handlers, security or tenancy combined), each written as a user story with acceptance criteria rather than Prefab vocabulary
- [x] #2 At least 6 brownfield tasks start from a copy of the showcase (TASK-337) or another non-trivial app: add a feature, change a contract with backward compatibility, evolve a schema with existing data, fix a failing test, find and fix a bug from a report
- [x] #3 Tasks are tagged by tier so `run` and `compare` report per tier; the existing 32 tasks stay as the quick tier
- [x] #4 The report adds time-to-first-green-build and the number of generated classes overridden or hand-written alternatives, alongside pass rate
- [x] #5 Baselines for Haiku (full docs, docs on demand) and a stronger model show a measurable gap on the new tiers, or the task records why there is none
- [x] #6 A documented, opt-in way to run the quick tier on a schedule (for example a manually triggered GitHub workflow with a repository secret) tracks regressions between releases
- [x] #7 `benchmark.md` describes the tiers and how to add a brownfield task
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
### Harness

- `Tier` enum (`quick`, `feature`, `brownfield`), `tier` and `base` in `task.yml`; `TaskResult.tier` (old reports read
  as `quick`); `--tasks` matches tiers; `list` shows tiers and counts.
- `BaseApp` (`benchmark/apps/<name>/app.yml` + `project/`): `WorkspaceFactory` overlays it before the starter, and
  its modules are merged into the task's. A brownfield task without a base, or with an unknown one, fails to load.
- `ProcessRunner.runRecordingLineTimes` writes `transcript.times` next to `transcript.jsonl`; `TranscriptParser`
  returns `firstGreenBuild` (first Maven `tool_result` that is not an error and shows no build or test failure), stored
  as `AgentOutcome.firstGreenBuild` (empty for old runs). `classify` recomputes it from kept transcripts.
- `TaskResult.overrides()` counts distinct files with `Guardrails.OVERRIDE_RULES` violations.
- `report.md`: summary rows for median time to first green build and overrides, a "By tier" table, tier, first green
  and overrides columns per attempt. `compare`: rows for both metrics and a pass-rate row per tier.

### Tasks (all validated: reference passes, starter fails for the intended reason)

Feature tier (empty project, user story with numbered acceptance criteria): `feature-library-loans`,
`feature-ticket-sales` (roles, public reads), `feature-helpdesk-tenancy` (tenancy, nested comments, async handler),
`feature-expense-approval` (three roles, `@CreatedBy`, create-from-event, events back), `feature-course-enrollment`
(multicast cascade over three events), `feature-auction-bidding` (rules in the record, audit, sale on closing),
`feature-shipment-tracking` (status machine, notify once), `feature-loyalty-points` (saga: request, decide, apply).

Brownfield tier on `benchmark/apps/shop` (a snapshot of the showcase core, `com.example.shop`, 36 passing tests,
committed `V1__generated.sql`): `brownfield-bug-deactivation` (bug report), `brownfield-fix-failing-test`
(`@Multicast` without `failWhenEmpty = false`), `brownfield-rename-contract` (`brand` to `manufacturer` with
`@DbRename` and a `@Computed` alias), `brownfield-evolve-schema` (required enum column with a default for existing
rows), `brownfield-product-reviews` and `brownfield-restock-requests` (features across existing aggregates). Schema
tasks check the migration scripts on the classpath, because the in-memory backend runs no migrations.

### Findings while writing the tasks

- TASK-386: the annotation reference documents `@DbRename(oldName = ...)` (does not compile) and quoted
  `@DbDefaultValue` strings (the quotes end up in the data).
- TASK-387: two equality `@Filter`s with different `parameter` names on one field generate duplicate variables. The
  rename task therefore only keeps `brand` in responses, not as a filter parameter.
- TASK-388: a generated migration re-adds a foreign key that a committed migration declared inline.
- Audit fields (`@CreatedBy`) are set after the `@Create` constructor, so an event published there cannot carry the
  user; the auction task refers to the winning bid instead.
- A nested resource's path comes from the type name (`Review` gives `/products/{id}/reviews`); there is no path
  override on `@Aggregate`.

### Baselines (AC #5)

Not recorded: this cloud session's permission policy denies starting nested Claude Code sessions, so no model could
be run. `benchmark.md` ("Feature and brownfield tiers: not yet recorded") gives the commands; TASK-389 tracks
recording Haiku (`full-docs`, `docs-folder`) and Sonnet with `--repeat 3`.

### Schedule (AC #6)

`.github/workflows/benchmark.yml`: manual dispatch (model, context, tasks, repeat; quick tier with Haiku and
`cheat-sheet` by default) and a weekly cron that only runs when the repository variable `BENCHMARK_SCHEDULE` is
`true`; needs the secret `ANTHROPIC_API_KEY`; writes the report summary to the job summary and uploads the results.
<!-- SECTION:NOTES:END -->
