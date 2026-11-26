# Prefab benchmark: qwen3-coder:30b with context `cheat-sheet`

| | |
|---|---|
| Run | 20261126-110041-qwen-local-cheat-sheet |
| Agent | claude-code/qwen3-coder:30b |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ a9b1d9bc-dirty |
| Started | 2026-11-26T11:00:41.626360Z |
| Attempts | 1 (1 tasks x 1) |
| **Pass rate** | **0/1 = 0% (95% CI 0-79%)** |
| Tasks passed in every attempt | 0 |
| Cost | $0,73 |
| Tokens (input incl. cache / output) | 932.035 / 6.036 |
| Median turns / builds / agent time | 49 / 7 / 2m44s |
| Median time to first green build | 41s (1/1 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 0/1 | 0% (95% CI 0-79%) | 49 | 41s (1/1 attempts) | 0 (in 0 attempts) |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 1 |
| PLAIN_SPRING_FALLBACK | 0 |
| TEST_OR_INFRASTRUCTURE | 0 |

## By task category

| Category | Passed |
|---|---|
| events-publishing | 0/1 |

## Attempts

| Task | Tier | # | Result | Checks | Turns | Builds | First green | Overrides | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| events-publish-on-create | quick | 1 | FAIL | +guardrails -hidden-tests | 49 | 7 | 41s | 0 | 932.035 / 6.036 | $0,73 | 2m44s | API_MISUSE |

## Failures

### events-publish-on-create#1

**API_MISUSE** (heuristic): the hidden tests do not compile against the generated API: names or shapes differ from the prompt

- `hidden-tests: hidden tests did not run (build or compile failure)`
- `build: [ERROR] /private/var/folders/z6/vvn2vyjd5v76z549q44zytf80000gn/T/prefab-benchmark/20261126-110041-qwen-local-cheat-sheet/events-publish-on-create-1/project/src/test/java/com/example/ordering/OrderPlacedAcceptanceTest.java:[20,19] cannot find symbol`

Logs: `/Users/stijnvanbael/IdeaProjects/appify/prefab/benchmark/results/20261126-110041-qwen-local-cheat-sheet/attempts/events-publish-on-create-1`
