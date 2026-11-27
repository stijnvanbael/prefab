# Prefab benchmark: qwen3.8:27b with context `local-run`

| | |
|---|---|
| Run | 20261126-172744-qwen38-sandboxed-local-run |
| Agent | claude-code/qwen3.8:27b |
| Context | local-run |
| Prefab | 0.13.3-SNAPSHOT @ 72640df8-dirty |
| Started | 2026-11-26T17:27:44.196051Z |
| Attempts | 6 (6 tasks x 1) |
| **Pass rate** | **6/6 = 100% (95% CI 61-100%)** |
| Tasks passed in every attempt | 6 |
| Cost | $5,29 |
| Tokens (input incl. cache / output) | 4.704.652 / 83.204 |
| Median turns / builds / agent time | 31 / 4 / 7m53s |
| Median time to first green build | 2m36s (6/6 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 6/6 | 100% (95% CI 61-100%) | 31 | 2m36s (6/6 attempts) | 0 (in 0 attempts) |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 0 |
| TEST_OR_INFRASTRUCTURE | 0 |

## By task category

| Category | Passed |
|---|---|
| events-handling | 1/1 |
| events-publishing | 1/1 |
| repository-mixins | 1/1 |
| rest-crud | 3/3 |

## Attempts

| Task | Tier | # | Result | Checks | Turns | Builds | First green | Overrides | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| events-multicast | quick | 1 | PASS | +guardrails +hidden-tests | 32 | 4 | 2m23s | 0 | 551.652 / 10.481 | $0,76 | 8m14s |  |
| events-publish-on-create | quick | 1 | PASS | +guardrails +hidden-tests | 60 | 7 | 8m33s | 0 | 1.710.657 / 23.473 | $1,60 | 15m40s |  |
| mixin-derived-query | quick | 1 | PASS | +guardrails +hidden-tests | 27 | 4 | 2m50s | 0 | 425.191 / 9.551 | $0,53 | 6m09s |  |
| rest-create-and-get | quick | 1 | PASS | +guardrails +hidden-tests | 29 | 4 | 53s | 0 | 639.656 / 17.105 | $0,97 | 14m26s |  |
| rest-parent-child | quick | 1 | PASS | +guardrails +hidden-tests | 43 | 4 | 3m25s | 0 | 959.073 / 12.547 | $0,89 | 7m33s |  |
| rest-validation | quick | 1 | PASS | +guardrails +hidden-tests | 29 | 3 | 1m24s | 0 | 418.423 / 10.047 | $0,53 | 5m58s |  |
