# Prefab benchmark: claude-haiku-5-5 with context `none`

| | |
|---|---|
| Run | 20261121-173455-claude-haiku-5-5-none |
| Agent | claude-code/claude-haiku-5-5 |
| Context | none |
| Prefab | 0.13.3-SNAPSHOT @ 26ff283 |
| Started | 2026-11-21T17:34:55.727680759Z |
| Attempts | 8 (8 tasks x 1) |
| **Pass rate** | **8/8 = 100% (95% CI 68-100%)** |
| Tasks passed in every attempt | 8 |
| Cost | $0.96 |
| Tokens (input incl. cache / output) | 19,341,205 / 250,350 |
| Median turns / builds / agent time | 43 / 12 / 5m05s |

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
| binary | 1/1 |
| events-handling | 1/1 |
| events-publishing | 1/1 |
| lists | 1/1 |
| persistence | 1/1 |
| rest-crud | 1/1 |
| security | 1/1 |
| testing | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 50 | 15 | 3,913,184 / 40,834 | $0.22 | 5m39s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 47 | 11 | 2,950,898 / 35,190 | $0.09 | 4m32s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 47 | 13 | 3,916,611 / 51,990 | $0.29 | 7m43s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 21 | 3 | 751,982 / 13,521 | $0.02 | 1m37s |  |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 28 | 9 | 1,329,644 / 19,724 | $0.03 | 2m51s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 50 | 16 | 4,191,944 / 49,717 | $0.25 | 6m13s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 38 | 18 | 1,960,299 / 33,808 | $0.05 | 5m43s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 12 | 3 | 326,643 / 5,566 | $0.01 | 42s |  |
