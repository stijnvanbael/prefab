# Prefab benchmark: qwen3-coder:30b with context `cheat-sheet`

| | |
|---|---|
| Run | 20261126-104823-qwen-local-cheat-sheet |
| Agent | claude-code/qwen3-coder:30b |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ a9b1d9bc-dirty |
| Started | 2026-11-26T10:48:23.665892Z |
| Attempts | 1 (1 tasks x 1) |
| **Pass rate** | **0/1 = 0% (95% CI 0-79%)** |
| Tasks passed in every attempt | 0 |
| Cost | $0,59 |
| Tokens (input incl. cache / output) | 714.502 / 4.725 |
| Median turns / builds / agent time | 35 / 10 / 2m27s |
| Median time to first green build | 59s (1/1 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 0/1 | 0% (95% CI 0-79%) | 35 | 59s (1/1 attempts) | 0 (in 0 attempts) |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 1 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 0 |
| TEST_OR_INFRASTRUCTURE | 0 |

## By task category

| Category | Passed |
|---|---|
| rest-crud | 0/1 |

## Attempts

| Task | Tier | # | Result | Checks | Turns | Builds | First green | Overrides | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| rest-create-and-get | quick | 1 | FAIL | +guardrails -hidden-tests | 35 | 10 | 59s | 0 | 714.502 / 4.725 | $0,59 | 2m27s | DOC_GAP |

## Failures

### rest-create-and-get#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 2/3 passed; com.example.catalog.BookAcceptanceTest.titleIsRequired: Status expected:<400> but was:<201>`

Logs: `/Users/stijnvanbael/IdeaProjects/appify/prefab/benchmark/results/20261126-104823-qwen-local-cheat-sheet/attempts/rest-create-and-get-1`
