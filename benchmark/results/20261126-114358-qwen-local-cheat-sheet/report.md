# Prefab benchmark: qwen3-coder:30b with context `cheat-sheet`

| | |
|---|---|
| Run | 20261126-114358-qwen-local-cheat-sheet |
| Agent | claude-code/qwen3-coder:30b |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ b8a0afab |
| Started | 2026-11-26T11:43:58.372891Z |
| Attempts | 1 (1 tasks x 1) |
| **Pass rate** | **0/1 = 0% (95% CI 0-79%)** |
| Tasks passed in every attempt | 0 |
| Cost | $0,71 |
| Tokens (input incl. cache / output) | 837.269 / 7.742 |
| Median turns / builds / agent time | 54 / 12 / 2m52s |
| Median time to first green build | 50s (1/1 attempts) |
| Generated classes overridden or hand-written | 0 (in 0 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 0/1 | 0% (95% CI 0-79%) | 54 | 50s (1/1 attempts) | 0 (in 0 attempts) |

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
| rest-create-and-get | quick | 1 | FAIL | +guardrails -hidden-tests | 54 | 12 | 50s | 0 | 837.269 / 7.742 | $0,71 | 2m52s | DOC_GAP |

## Failures

### rest-create-and-get#1

**DOC_GAP** (manual): Imported be.appify.prefab.core.Reference (actual package core.service), decided Reference was generated, replaced it with a String id set to null, so POST returned 500. The cheat sheet did not give the package; fixed by the import table. 

- `hidden-tests: 1/3 passed; com.example.catalog.BookAcceptanceTest.titleIsRequired: Status expected:<400> but was:<500> | com.example.catalog.BookAcceptanceTest.createdBookCanBeRetrieved: Status expected:<201> but was:<500>`

Logs: `/Users/stijnvanbael/IdeaProjects/appify/prefab/benchmark/results/20261126-114358-qwen-local-cheat-sheet/attempts/rest-create-and-get-1`
