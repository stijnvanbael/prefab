# Prefab benchmark: qwen3.8:27b with context `none`

| | |
|---|---|
| Run | 20261126-204151-qwen38-sandboxed-none |
| Agent | claude-code/qwen3.8:27b |
| Context | none |
| Prefab | 0.13.3-SNAPSHOT @ 72640df8-dirty |
| Started | 2026-11-26T20:41:51.954316Z |
| Attempts | 6 (6 tasks x 1) |
| **Pass rate** | **6/6 = 100% (95% CI 61-100%)** |
| Tasks passed in every attempt | 6 |
| Cost | $12,21 |
| Tokens (input incl. cache / output) | 16.980.270 / 171.774 |
| Median turns / builds / agent time | 72 / 8 / 30m46s |
| Median time to first green build | 9m36s (6/6 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 6/6 | 100% (95% CI 61-100%) | 72 | 9m36s (6/6 attempts) | 0 (in 0 attempts) |

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
| events-multicast | quick | 1 | PASS | +guardrails +hidden-tests | 78 | 5 | 21m58s | 0 | 4.134.806 / 52.366 | $3,68 | 35m12s |  |
| events-publish-on-create | quick | 1 | PASS | +guardrails +hidden-tests | 75 | 15 | 14m17s | 0 | 4.329.929 / 50.070 | $3,77 | 38m30s |  |
| mixin-derived-query | quick | 1 | PASS | +guardrails +hidden-tests | 45 | 10 | 1m06s | 0 | 1.009.334 / 20.937 | $1,13 | 11m35s |  |
| rest-create-and-get | quick | 1 | PASS | +guardrails +hidden-tests | 73 | 4 | 25m03s | 0 | 3.816.059 / 0 | $0,00 | 90m30s |  |
| rest-parent-child | quick | 1 | PASS | +guardrails +hidden-tests | 70 | 7 | 4m56s | 0 | 2.803.577 / 33.708 | $2,72 | 26m21s |  |
| rest-validation | quick | 1 | PASS | +guardrails +hidden-tests | 41 | 8 | 1m31s | 0 | 886.565 / 14.693 | $0,92 | 8m31s |  |
