# Prefab benchmark: qwen3.8:27b with context `cheat-sheet`

| | |
|---|---|
| Run | 20261126-120036-qwen38-local-cheat-sheet |
| Agent | claude-code/qwen3.8:27b |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ e3fc826a-dirty |
| Started | 2026-11-26T12:00:36.618448Z |
| Attempts | 1 (1 tasks x 1) |
| **Pass rate** | **1/1 = 100% (95% CI 21-100%)** |
| Tasks passed in every attempt | 1 |
| Cost | $0,46 |
| Tokens (input incl. cache / output) | 279.819 / 9.537 |
| Median turns / builds / agent time | 23 / 3 / 4m04s |
| Median time to first green build | 1m21s (1/1 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 1/1 | 100% (95% CI 21-100%) | 23 | 1m21s (1/1 attempts) | 0 (in 0 attempts) |

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
| rest-crud | 1/1 |

## Attempts

| Task | Tier | # | Result | Checks | Turns | Builds | First green | Overrides | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| rest-create-and-get | quick | 1 | PASS | +guardrails +hidden-tests | 23 | 3 | 1m21s | 0 | 279.819 / 9.537 | $0,46 | 4m04s |  |
