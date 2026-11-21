# Prefab benchmark: claude-sonnet-5-5 with context `full-docs`

| | |
|---|---|
| Run | 20261121-180102-claude-sonnet-5-5-full-docs |
| Agent | claude-code/claude-sonnet-5-5 |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ 26ff283-dirty |
| Started | 2026-11-21T18:01:02.231019796Z |
| Attempts | 8 (8 tasks x 1) |
| **Pass rate** | **6/8 = 75% (95% CI 41-93%)** |
| Tasks passed in every attempt | 6 |
| Cost | $3.99 |
| Tokens (input incl. cache / output) | 6,375,614 / 21,378 |
| Median turns / builds / agent time | 6 / 3 / 29s |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 1 |
| PLAIN_SPRING_FALLBACK | 0 |
| TEST_OR_INFRASTRUCTURE | 1 |

## By task category

| Category | Passed |
|---|---|
| binary | 1/1 |
| events-handling | 1/1 |
| events-publishing | 1/1 |
| lists | 0/1 |
| persistence | 1/1 |
| rest-crud | 1/1 |
| security | 0/1 |
| testing | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 15 | 5 | 1,775,433 / 6,159 | $0.67 | 1m44s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 3 | 1 | 327,918 / 1,043 | $0.42 | 15s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 5 | 1 | 551,343 / 1,954 | $0.45 | 20s |  |
| list-range-filter | 1 | FAIL | +guardrails -hidden-tests | 3 | 1 | 327,382 / 1,133 | $0.42 | 16s | TEST_OR_INFRASTRUCTURE |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 6 | 4 | 664,178 / 1,981 | $0.47 | 36s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 6 | 4 | 671,041 / 2,359 | $0.48 | 51s |  |
| security-public-endpoints | 1 | FAIL | +guardrails -hidden-tests | 13 | 6 | 1,503,247 / 4,974 | $0.62 | 1m27s | API_MISUSE |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 5 | 2 | 555,072 / 1,775 | $0.45 | 23s |  |

## Failures

### list-range-filter#1

**TEST_OR_INFRASTRUCTURE** (manual): The Postgres test container died during the hidden tests ("terminating connection due to unexpected postmaster exit"). The code matches the reference; the agent did not run any build or test itself (3 turns). 

- `hidden-tests: 0/3 passed; com.example.billing.InvoiceRangeAcceptanceTest.filtersByIssueDateAndAmount: Failed to load ApplicationContext for [WebMergedContextConfiguration@2c4eda3a testClass = com.example.billing.InvoiceRangeAcceptanceTest, locations = [], classes = [com.example.Application], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors = [], propertySourcePr...`

Logs: `/home/user/prefab/benchmark/results/20261121-180102-claude-sonnet-5-5-full-docs/attempts/list-range-filter-1`

### security-public-endpoints#1

**API_MISUSE** (manual): Same as Haiku: an extra @Order(HIGHEST_PRECEDENCE) SecurityFilterChain for the public GETs disables Prefab's @ConditionalOnMissingBean default chain, so authenticated writes get 401 (TASK-353). 

- `hidden-tests: 1/2 passed; com.example.blog.PublicArticlesAcceptanceTest.anonymousReadersSeeArticles: Status expected:<201> but was:<401>`

Logs: `/home/user/prefab/benchmark/results/20261121-180102-claude-sonnet-5-5-full-docs/attempts/security-public-endpoints-1`
