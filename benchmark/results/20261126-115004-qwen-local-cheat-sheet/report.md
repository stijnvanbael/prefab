# Prefab benchmark: qwen3-coder:30b with context `cheat-sheet`

| | |
|---|---|
| Run | 20261126-115004-qwen-local-cheat-sheet |
| Agent | claude-code/qwen3-coder:30b |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ dcb6e25c |
| Started | 2026-11-26T11:50:04.791298Z |
| Attempts | 1 (1 tasks x 1) |
| **Pass rate** | **0/1 = 0% (95% CI 0-79%)** |
| Tasks passed in every attempt | 0 |
| Cost | $0,47 |
| Tokens (input incl. cache / output) | 530.809 / 5.872 |
| Median turns / builds / agent time | 38 / 9 / 1m59s |
| Median time to first green build | 19s (1/1 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 0/1 | 0% (95% CI 0-79%) | 38 | 19s (1/1 attempts) | 0 (in 0 attempts) |

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
| rest-create-and-get | quick | 1 | FAIL | +guardrails -hidden-tests | 38 | 9 | 19s | 0 | 530.809 / 5.872 | $0,47 | 1m59s | DOC_GAP |

## Failures

### rest-create-and-get#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 1/3 passed; com.example.catalog.BookAcceptanceTest.titleIsRequired: Status expected:<400> but was:<404> | com.example.catalog.BookAcceptanceTest.createdBookCanBeRetrieved: Status expected:<201> but was:<404>`

Logs: `/Users/stijnvanbael/IdeaProjects/appify/prefab/benchmark/results/20261126-115004-qwen-local-cheat-sheet/attempts/rest-create-and-get-1`
