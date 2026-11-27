# Prefab benchmark: qwen3.8:27b with context `full-docs`

| | |
|---|---|
| Run | 20261126-182614-qwen38-sandboxed-full-docs |
| Agent | claude-code/qwen3.8:27b |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ 72640df8-dirty |
| Started | 2026-11-26T18:26:14.355068Z |
| Attempts | 6 (6 tasks x 1) |
| **Pass rate** | **6/6 = 100% (95% CI 61-100%)** |
| Tasks passed in every attempt | 6 |
| Cost | $10,55 |
| Tokens (input incl. cache / output) | 14.891.036 / 84.055 |
| Median turns / builds / agent time | 24 / 7 / 17m52s |
| Median time to first green build | 9m52s (6/6 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 6/6 | 100% (95% CI 61-100%) | 24 | 9m52s (6/6 attempts) | 0 (in 0 attempts) |

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
| events-multicast | quick | 1 | PASS | +guardrails +hidden-tests | 52 | 11 | 8m55s | 0 | 4.395.461 / 23.465 | $3,17 | 44m25s |  |
| events-publish-on-create | quick | 1 | PASS | +guardrails +hidden-tests | 23 | 5 | 11m36s | 0 | 1.820.586 / 9.363 | $1,26 | 15m24s |  |
| mixin-derived-query | quick | 1 | PASS | +guardrails +hidden-tests | 21 | 7 | 9m55s | 0 | 1.494.294 / 6.957 | $1,00 | 12m26s |  |
| rest-create-and-get | quick | 1 | PASS | +guardrails +hidden-tests | 23 | 6 | 9m00s | 0 | 1.553.809 / 11.840 | $1,17 | 15m40s |  |
| rest-parent-child | quick | 1 | PASS | +guardrails +hidden-tests | 38 | 7 | 12m51s | 0 | 3.461.135 / 17.385 | $2,27 | 20m05s |  |
| rest-validation | quick | 1 | PASS | +guardrails +hidden-tests | 24 | 8 | 9m50s | 0 | 2.165.751 / 15.045 | $1,69 | 27m07s |  |
