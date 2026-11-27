# Prefab benchmark: qwen3.8:27b with context `cheat-sheet`

| | |
|---|---|
| Run | 20261127-101327-qwen38-local-cheat-sheet |
| Agent | claude-code/qwen3.8:27b |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ bc4a1669 |
| Started | 2026-11-27T10:13:27.914073Z |
| Attempts | 6 (6 tasks x 1) |
| **Pass rate** | **5/6 = 83% (95% CI 44-97%)** |
| Tasks passed in every attempt | 5 |
| Cost | $4.26 |
| Tokens (input incl. cache / output) | 3,804,187 / 68,901 |
| Median turns / builds / agent time | 30 / 5 / 5m51s |
| Median time to first green build | 1m55s (6/6 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |
| **Contaminated attempts** | events-multicast#1 |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 5/6 | 83% (95% CI 44-97%) | 30 | 1m55s (6/6 attempts) | 0 (in 0 attempts) |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 0 |
| TEST_OR_INFRASTRUCTURE | 1 |

## By task category

| Category | Passed |
|---|---|
| events-handling | 0/1 |
| events-publishing | 1/1 |
| repository-mixins | 1/1 |
| rest-crud | 3/3 |

## Attempts

| Task | Tier | # | Result | Checks | Turns | Builds | First green | Overrides | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| events-multicast | quick | 1 | FAIL | +guardrails +hidden-tests | 46 | 8 | 1m22s | 0 | 996,804 / 15,931 | $1.02 | 8m50s | TEST_OR_INFRASTRUCTURE |
| events-publish-on-create | quick | 1 | PASS | +guardrails +hidden-tests | 31 | 3 | 4m00s | 0 | 521,810 / 6,714 | $0.54 | 4m35s |  |
| mixin-derived-query | quick | 1 | PASS | +guardrails +hidden-tests | 38 | 6 | 2m01s | 0 | 1,016,935 / 18,849 | $1.16 | 10m43s |  |
| rest-create-and-get | quick | 1 | PASS | +guardrails +hidden-tests | 29 | 5 | 1m50s | 0 | 603,568 / 14,157 | $0.76 | 7m08s |  |
| rest-parent-child | quick | 1 | PASS | +guardrails +hidden-tests | 26 | 4 | 2m08s | 0 | 382,907 / 5,929 | $0.41 | 3m18s |  |
| rest-validation | quick | 1 | PASS | +guardrails +hidden-tests | 19 | 5 | 1m02s | 0 | 282,163 / 7,321 | $0.38 | 3m16s |  |

## Failures

### events-multicast#1

**TEST_OR_INFRASTRUCTURE** (heuristic): the agent read the benchmark sources (hidden tests or reference)


Logs: `/Users/stijnvanbael/IdeaProjects/appify/prefab/benchmark/results/20261127-101327-qwen38-local-cheat-sheet/attempts/events-multicast-1`
