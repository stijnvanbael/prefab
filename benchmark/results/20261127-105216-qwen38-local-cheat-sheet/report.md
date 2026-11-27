# Prefab benchmark: qwen3.8:27b with context `cheat-sheet`

| | |
|---|---|
| Run | 20261127-105216-qwen38-local-cheat-sheet |
| Agent | claude-code/qwen3.8:27b |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ bc4a1669-dirty |
| Started | 2026-11-27T10:52:16.221772Z |
| Attempts | 1 (1 tasks x 1) |
| **Pass rate** | **1/1 = 100% (95% CI 21-100%)** |
| Tasks passed in every attempt | 1 |
| Cost | $1.48 |
| Tokens (input incl. cache / output) | 1,571,155 / 20,278 |
| Median turns / builds / agent time | 53 / 4 / 14m20s |
| Median time to first green build | 6m55s (1/1 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 1/1 | 100% (95% CI 21-100%) | 53 | 6m55s (1/1 attempts) | 0 (in 0 attempts) |

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

## Attempts

| Task | Tier | # | Result | Checks | Turns | Builds | First green | Overrides | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| events-multicast | quick | 1 | PASS | +guardrails +hidden-tests | 53 | 4 | 6m55s | 0 | 1,571,155 / 20,278 | $1.48 | 14m20s |  |
