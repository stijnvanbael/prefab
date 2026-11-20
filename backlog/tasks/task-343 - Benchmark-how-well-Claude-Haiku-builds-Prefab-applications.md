---
id: TASK-343
title: Benchmark how well Claude Haiku builds Prefab applications
status: To Do
assignee: []
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
- [ ] #1 A set of at least 30 benchmark tasks covers the main Prefab use cases (REST CRUD, filters and paging, publishing and handling events, repository mixins, security, multi-tenancy, binary fields, Avro, testing), each with a starting project, a natural-language prompt and hidden acceptance tests the model cannot see
- [ ] #2 A single command runs the benchmark against a chosen model and context setup, and reports per task pass/fail, tokens used, number of turns/retries and wall-clock time
- [ ] #3 Baseline results are recorded for Haiku with at least: no Prefab docs, the full current docs, and (once they exist) the curated agent context; Sonnet or Opus results are recorded as a reference ceiling
- [ ] #4 Failures are classified (doc gap, misleading diagnostic, API misuse, fell back to plain Spring/JPA code, test or infrastructure problem) so each one can be turned into a follow-up
- [ ] #5 A run never needs real cloud credentials, and runs are reproducible enough that the same setup gives a pass rate within a documented margin
- [ ] #6 How to run the benchmark, add a task and read the report is documented in backlog/docs
<!-- AC:END -->
