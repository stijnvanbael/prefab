# Prefab benchmark: qwen3.8:27b with context `recipes`

| | |
|---|---|
| Run | 20261126-165026-qwen38-sandboxed-recipes |
| Agent | claude-code/qwen3.8:27b |
| Context | recipes |
| Prefab | 0.13.3-SNAPSHOT @ 72640df8-dirty |
| Started | 2026-11-26T16:50:26.317695Z |
| Attempts | 6 (6 tasks x 1) |
| **Pass rate** | **6/6 = 100% (95% CI 61-100%)** |
| Tasks passed in every attempt | 6 |
| Cost | $3,66 |
| Tokens (input incl. cache / output) | 3.224.679 / 61.726 |
| Median turns / builds / agent time | 33 / 5 / 5m30s |
| Median time to first green build | 2m27s (6/6 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 6/6 | 100% (95% CI 61-100%) | 33 | 2m27s (6/6 attempts) | 0 (in 0 attempts) |

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
| events-multicast | quick | 1 | PASS | +guardrails +hidden-tests | 39 | 6 | 2m48s | 0 | 709.611 / 11.066 | $0,72 | 6m25s |  |
| events-publish-on-create | quick | 1 | PASS | +guardrails +hidden-tests | 46 | 4 | 4m34s | 0 | 735.624 / 12.628 | $0,79 | 7m39s |  |
| mixin-derived-query | quick | 1 | PASS | +guardrails +hidden-tests | 26 | 5 | 1m26s | 0 | 329.294 / 8.211 | $0,42 | 4m20s |  |
| rest-create-and-get | quick | 1 | PASS | +guardrails +hidden-tests | 23 | 3 | 1m57s | 0 | 365.073 / 7.389 | $0,45 | 4m36s |  |
| rest-parent-child | quick | 1 | PASS | +guardrails +hidden-tests | 41 | 6 | 5m18s | 0 | 820.119 / 15.815 | $0,90 | 9m25s |  |
| rest-validation | quick | 1 | PASS | +guardrails +hidden-tests | 20 | 3 | 2m07s | 0 | 264.958 / 6.617 | $0,39 | 4m22s |  |
