---
id: TASK-343
title: Benchmark how well Claude Haiku builds Prefab applications
status: In Progress
assignee:
  - '@claude'
created_date: '2026-11-20 20:13'
labels:
  - agents
  - dx
  - testing
dependencies: []
priority: high
type: task
ordinal: 188000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab's goal is shifting from helping humans develop to helping AI agents develop. Because Prefab apps have small codebases, a fast, cheap model like Claude Haiku should be able to do most of the work. Today Haiku doesn't know Prefab, and the readme, developer guide (backlog/docs, about 214 KB or 55k tokens) and source together are too much for it to stay focused.

Every other "AI-ready" improvement (agent cheat sheet, task recipes, per-project agent context, guardrails, runtime diagnostics) is a guess until we can measure it. This task builds a repeatable benchmark: realistic app-building tasks, each checked by hidden acceptance tests, run with Haiku under different documentation setups. Every failure points to a doc gap, a weak diagnostic, or an API that is easy to misuse. That makes "Haiku develops Prefab apps flawlessly" a number we can track.

Haiku has a large context window, so the benchmark must also test whether curated context actually beats giving it the full docs. Don't assume it does.

Good sources of task material: the annotation-processor fixtures (annotation-processor/src/test/resources/**/source and expected), the example modules, and the shop showcase planned in TASK-337.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A set of at least 30 benchmark tasks covers the main Prefab use cases (REST CRUD, filters and paging, publishing and handling events, repository mixins, security, multi-tenancy, binary fields, Avro, testing), each with a starting project, a natural-language prompt and hidden acceptance tests the model cannot see
- [x] #2 A single command runs the benchmark against a chosen model and context setup, and reports per task pass/fail, tokens used, number of turns/retries and wall-clock time
- [ ] #3 Baseline results are recorded for Haiku with at least: no Prefab docs, the full current docs, and (once they exist) the curated agent context; Sonnet or Opus results are recorded as a reference ceiling
- [x] #4 Failures are classified (doc gap, misleading diagnostic, API misuse, fell back to plain Spring/JPA code, test or infrastructure problem) so each one can be turned into a follow-up
- [ ] #5 A run never needs real cloud credentials, and runs are reproducible enough that the same setup gives a pass rate within a documented margin
- [x] #6 How to run the benchmark, add a task and read the report is documented in backlog/docs
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Analysis (2026-11-21):
- The `claude` CLI runs headless (`claude -p --output-format stream-json`) and its final `result` event reports
  tokens, turns, cost and duration, so Claude Code itself is the agent under test. Haiku and Sonnet/Opus are
  selected with `--model`.
- Prefab integration tests need only Docker (Testcontainers for Postgres, Kafka, schema registry, MongoDB,
  LocalStack). No real cloud credentials are involved anywhere.
- prefab-test reuses containers with fixed names (`prefab_<type>`), so concurrent runs would share Kafka topics.
  The harness gives every parallel slot its own container and network names through the
  `prefab.test.*.container-name` properties (passed as environment variables, so the agent's own builds get them too).

Design:
1. New Maven module `benchmark/` (Java harness, built with the reactor so it never rots) plus data:
   `benchmark/template` (standalone app skeleton), `benchmark/tasks/<id>/` (task.yml, prompt.md, starter/,
   hidden/, reference/, optional mutants/), `benchmark/contexts/<name>/` (documentation setups),
   `benchmark/results/` (recorded baselines).
2. Per run: copy template + starter into a temp workspace outside the repo, apply the context setup, git-commit
   the baseline, run the agent, then copy in the hidden acceptance tests and run them with Maven. Static
   guardrail checks reject hand-written controllers, JPA entities, hand-written migrations, Lombok etc.
   Testing tasks are checked by running the agent's tests against the correct code and against mutants.
3. Agents: `claude` (Claude Code CLI), `reference` (applies the reference solution) and `noop` (leaves the
   starter). `validate` = reference must pass and noop must fail for every task, which keeps tasks honest.
4. Report: JSON + Markdown per run with pass/fail, checks, tokens, cost, turns, wall-clock, suggested failure
   category from heuristics, optional LLM triage, manual overrides, and a `compare` command across runs.
   Repeats give a pass-rate interval; the reproducibility margin is measured and documented.
5. Tasks (30+) cover REST CRUD, validation, value objects, computed fields, parent/child, filters, range,
   sorting/paging, full-text, publishing and handling events, repository mixins, security, multi-tenancy,
   audit, binary fields, Avro, MongoDB and testing.
6. Docs in backlog/docs/benchmark.md; baselines for Haiku (no docs, full docs) and a stronger reference model.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Delivered (see backlog/docs/benchmark.md for the full guide):

- `benchmark/` Maven module (in the reactor, so it is compiled and unit-tested with every build): a Java harness
  run through `benchmark/run.sh` with the commands `run`, `validate`, `list`, `compare` and `classify`.
- **32 tasks** in `benchmark/tasks/`: REST CRUD (7), lists/filters/paging/full-text/autocomplete (5), persistence
  (1), events publishing (2) and handling (3), repository mixins (2), security (2), multi-tenancy, audit, binary,
  Avro (2), MongoDB, views, diagnostics, and testing (2, checked with mutants). Each has a starter, a prompt, hidden
  HTTP-level acceptance tests and a reference solution. `validate` proves that every reference passes and every
  starter fails; all 32 validate.
- Agents: Claude Code headless (`claude -p`), plus `reference` and `noop` stand-ins. The report has pass/fail, checks,
  tokens, cost, turns, Maven builds (the retry proxy) and wall-clock time, with Wilson 95% CIs. Failure
  classification uses heuristics, then optional LLM triage (`--triage-model`), then manual `classifications.yml`.
- Guardrails catch plain-Spring fallbacks (controllers, JPA, Spring Data repositories, JdbcTemplate, Lombok,
  overriding generated classes; a new SQL migration is a warning).
- Contexts: `none`, `full-docs` (60k tokens in CLAUDE.md) and `docs-folder` (read on demand). Add contexts for
  TASK-344/346/350 as they land.

Lessons built into the harness:
- Haiku searched the file system and read the reference solution from the repo checkout. The agent now runs in a
  private mount namespace with the repo and other workspaces hidden, and tool calls are still scanned for contamination.
- With the example jars installed in ~/.m2, agents decompiled them, so `run.sh` installs Prefab without the examples.
- Reused Testcontainers kept Kafka messages between attempts (poison pills). Slot containers are now removed before
  each attempt and before acceptance, with per-run names.
- A looping test consumer wrote 575 MB of log, so build logs are now capped and streamed.
- Two parallel runs used up a subscription 5-hour limit. HTTP 429 now stops the run (exit code 3) instead of
  recording bogus failures.

Prefab issues found while building the tasks, filed as TASK-351 (processor failures: duplicate @Valid, @Filter on
boolean, AVSC namespace), TASK-352 (upload constraints not enforced, wrong multipart docs), TASK-353 (no way to
permit public endpoints, plus the extra-SecurityFilterChain trap from the baseline) and TASK-354 (event doc
pitfalls, test consumer log flood). Each workaround is noted in the affected task.yml.

Baseline (2026-11-21, 8-task subset chosen to fit the usage limit): Haiku none 8/8, Haiku full-docs 7/8, Sonnet
full-docs 6/8 (one infrastructure failure). Docs cut Haiku's turns from 43 to 12 and its time from 5m to 1m10s, but
did not change correctness measurably. Details are in backlog/docs/benchmark.md and benchmark/results/.

Open for AC #3: the full 32-task baselines and the `docs-folder` setup (run with an API key), and the curated context
once TASK-344 exists. AC #5: the statistical margin is documented, but the empirical run-to-run margin needs a
repeated run (`--repeat 3`).
<!-- SECTION:NOTES:END -->
