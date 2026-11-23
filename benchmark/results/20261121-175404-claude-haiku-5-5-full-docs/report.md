# Prefab benchmark: claude-haiku-5-5 with context `full-docs`

| | |
|---|---|
| Run | 20261121-175404-claude-haiku-5-5-full-docs |
| Agent | claude-code/claude-haiku-5-5 |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ 26ff283-dirty |
| Started | 2026-11-21T17:54:04.240908421Z |
| Attempts | 8 (8 tasks x 1) |
| **Pass rate** | **7/8 = 88% (95% CI 53-98%)** |
| Tasks passed in every attempt | 7 |
| Cost | $1.74 |
| Tokens (input incl. cache / output) | 14,080,984 / 73,786 |
| Median turns / builds / agent time | 12 / 4 / 1m10s |

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
| binary | 1/1 |
| events-handling | 1/1 |
| events-publishing | 1/1 |
| lists | 1/1 |
| persistence | 1/1 |
| rest-crud | 1/1 |
| security | 0/1 |
| testing | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 20 | 4 | 2,459,381 / 15,551 | $0.28 | 2m05s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 12 | 4 | 1,403,260 / 8,168 | $0.19 | 1m14s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 15 | 2 | 1,634,657 / 7,231 | $0.20 | 1m06s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 12 | 3 | 1,274,173 / 6,009 | $0.18 | 54s |  |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 7 | 3 | 794,309 / 4,724 | $0.15 | 46s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 12 | 5 | 1,431,599 / 9,461 | $0.21 | 1m21s |  |
| security-public-endpoints | 1 | FAIL | +guardrails -hidden-tests | 33 | 9 | 4,406,905 / 18,872 | $0.40 | 2m40s | API_MISUSE |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 6 | 1 | 676,700 / 3,770 | $0.14 | 24s |  |

## Failures

### security-public-endpoints#1

**API_MISUSE** (manual): Added a second, path-scoped SecurityFilterChain for the public GETs. Prefab's default chain is @ConditionalOnMissingBean, so it silently disappeared: no chain handles POST any more, bearer tokens are never processed and every authenticated write gets 401. The agent's own tests only checked anonymous requests (TASK-353). 

- `hidden-tests: 1/2 passed; com.example.blog.PublicArticlesAcceptanceTest.anonymousReadersSeeArticles: Status expected:<201> but was:<401>`

Logs: `/home/user/prefab/benchmark/results/20261121-175404-claude-haiku-5-5-full-docs/attempts/security-public-endpoints-1`
