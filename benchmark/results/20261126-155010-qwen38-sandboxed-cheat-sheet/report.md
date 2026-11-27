# Prefab benchmark: qwen3.8:27b with context `cheat-sheet`

| | |
|---|---|
| Run | 20261126-155010-qwen38-sandboxed-cheat-sheet |
| Agent | claude-code/qwen3.8:27b |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ dba8838f |
| Started | 2026-11-26T15:50:10.692557Z |
| Attempts | 6 (6 tasks x 1) |
| **Pass rate** | **6/6 = 100% (95% CI 61-100%)** |
| Tasks passed in every attempt | 6 |
| Cost | $5,70 |
| Tokens (input incl. cache / output) | 5.038.998 / 98.980 |
| Median turns / builds / agent time | 31 / 5 / 8m32s |
| Median time to first green build | 3m44s (6/6 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 6/6 | 100% (95% CI 61-100%) | 31 | 3m44s (6/6 attempts) | 0 (in 0 attempts) |

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
| events-multicast | quick | 1 | PASS | +guardrails +hidden-tests | 54 | 8 | 8m03s | 0 | 1.697.872 / 28.157 | $1,74 | 17m46s |  |
| events-publish-on-create | quick | 1 | PASS | +guardrails +hidden-tests | 37 | 4 | 8m18s | 0 | 1.114.788 / 20.764 | $1,24 | 13m12s |  |
| mixin-derived-query | quick | 1 | PASS | +guardrails +hidden-tests | 26 | 6 | 1m06s | 0 | 470.402 / 13.897 | $0,66 | 7m36s |  |
| rest-create-and-get | quick | 1 | PASS | +guardrails +hidden-tests | 26 | 5 | 55s | 0 | 364.626 / 9.978 | $0,51 | 5m42s |  |
| rest-parent-child | quick | 1 | PASS | +guardrails +hidden-tests | 35 | 4 | 6m00s | 0 | 978.950 / 14.629 | $1,01 | 9m29s |  |
| rest-validation | quick | 1 | PASS | +guardrails +hidden-tests | 25 | 5 | 1m28s | 0 | 412.360 / 11.555 | $0,55 | 6m01s |  |
