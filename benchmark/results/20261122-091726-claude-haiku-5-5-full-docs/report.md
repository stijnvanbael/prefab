# Prefab benchmark: claude-haiku-5-5 with context `full-docs`

| | |
|---|---|
| Run | 20261122-091726-claude-haiku-5-5-full-docs |
| Agent | claude-code/claude-haiku-5-5 |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ fa2b6e9 |
| Started | 2026-11-22T09:17:26.761558326Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **31/32 = 97% (95% CI 84-99%)** |
| Tasks passed in every attempt | 31 |
| Cost | $6.42 |
| Tokens (input incl. cache / output) | 50,127,436 / 262,236 |
| Median turns / builds / agent time | 13 / 3 / 1m06s |

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
| audit | 0/1 |
| avro | 2/2 |
| binary | 1/1 |
| diagnostics | 1/1 |
| events-handling | 3/3 |
| events-publishing | 2/2 |
| lists | 5/5 |
| mongodb | 1/1 |
| multi-tenancy | 1/1 |
| persistence | 1/1 |
| repository-mixins | 2/2 |
| rest-crud | 7/7 |
| security | 2/2 |
| testing | 2/2 |
| views | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| audit-trail | 1 | FAIL | +guardrails -hidden-tests | 15 | 5 | 1,841,719 / 13,119 | $0.15 | 1m42s | TEST_OR_INFRASTRUCTURE |
| avro-avsc-contract | 1 | PASS | +guardrails +hidden-tests | 16 | 6 | 1,932,561 / 9,221 | $0.14 | 1m58s |  |
| avro-publish-and-handle | 1 | PASS | +guardrails +hidden-tests | 11 | 3 | 1,298,090 / 6,978 | $0.19 | 1m20s |  |
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 22 | 7 | 2,904,527 / 19,121 | $0.33 | 2m38s |  |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 17 | 6 | 1,865,143 / 9,801 | $0.23 | 1m58s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 18 | 8 | 2,258,379 / 12,339 | $0.26 | 2m20s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 26 | 5 | 2,214,984 / 14,116 | $0.26 | 2m03s |  |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 13 | 2 | 1,386,805 / 6,533 | $0.19 | 1m01s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 14 | 3 | 1,562,571 / 9,085 | $0.21 | 1m25s |  |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 10 | 2 | 1,055,391 / 7,397 | $0.17 | 1m01s |  |
| list-autocomplete | 1 | PASS | +guardrails +hidden-tests | 13 | 3 | 1,501,529 / 5,386 | $0.19 | 48s |  |
| list-filters | 1 | PASS | +guardrails +hidden-tests | 15 | 5 | 1,771,356 / 8,136 | $0.21 | 1m11s |  |
| list-full-text-search | 1 | PASS | +guardrails +hidden-tests | 12 | 4 | 1,398,755 / 7,903 | $0.19 | 1m05s |  |
| list-paged-sorted | 1 | PASS | +guardrails +hidden-tests | 10 | 1 | 1,148,214 / 5,377 | $0.17 | 43s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 17 | 3 | 2,001,876 / 7,519 | $0.22 | 1m06s |  |
| mixin-custom-sql | 1 | PASS | +guardrails +hidden-tests | 10 | 3 | 1,155,396 / 5,754 | $0.17 | 1m03s |  |
| mixin-derived-query | 1 | PASS | +guardrails +hidden-tests | 12 | 3 | 1,143,836 / 6,480 | $0.17 | 55s |  |
| mongodb-crud | 1 | PASS | +guardrails +hidden-tests | 12 | 4 | 1,398,077 / 6,390 | $0.19 | 1m16s |  |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 9 | 3 | 1,044,247 / 5,692 | $0.17 | 56s |  |
| rest-computed-field | 1 | PASS | +guardrails +hidden-tests | 12 | 4 | 1,387,115 / 6,727 | $0.19 | 59s |  |
| rest-create-and-get | 1 | PASS | +guardrails +hidden-tests | 9 | 3 | 1,035,497 / 5,518 | $0.17 | 57s |  |
| rest-custom-action | 1 | PASS | +guardrails +hidden-tests | 8 | 2 | 922,355 / 4,569 | $0.16 | 45s |  |
| rest-parent-child | 1 | PASS | +guardrails +hidden-tests | 15 | 4 | 1,776,510 / 7,992 | $0.22 | 1m07s |  |
| rest-update-and-delete | 1 | PASS | +guardrails +hidden-tests | 12 | 2 | 1,034,761 / 4,969 | $0.16 | 42s |  |
| rest-validation | 1 | PASS | +guardrails +hidden-tests | 20 | 6 | 2,131,772 / 10,123 | $0.24 | 1m37s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 9 | 2 | 1,049,175 / 6,632 | $0.17 | 48s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 20 | 3 | 2,521,168 / 13,258 | $0.29 | 1m42s |  |
| security-role-based | 1 | PASS | +guardrails +hidden-tests | 13 | 5 | 1,546,352 / 7,161 | $0.20 | 1m22s |  |
| tenancy-isolation | 1 | PASS | +guardrails +hidden-tests | 13 | 3 | 1,554,623 / 9,443 | $0.21 | 1m12s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 8 | 2 | 803,924 / 4,520 | $0.15 | 30s |  |
| testing-integration-tests | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +agent-tests-pass +mutant-killed:pay-keeps-status +mutant-killed:double-payment-allowed +mutant-killed:amount-not-stored | 15 | 3 | 1,815,394 / 7,969 | $0.22 | 1m14s |  |
| view-model | 1 | PASS | +guardrails +hidden-tests | 15 | 2 | 1,665,334 / 7,008 | $0.21 | 56s |  |

## Failures

### audit-trail#1

**TEST_OR_INFRASTRUCTURE** (manual): The hidden test failed with "Could not open JDBC Connection": the database was gone. The agent's own two tests passed. This was the first attempt after a resume; processes of the previous, rate-limited invocation were not killed and shared its container names (both fixed afterwards). 

- `hidden-tests: 0/1 passed; com.example.legal.ContractAuditAcceptanceTest.recordsCreatorAndLastModifier: Status expected:<201> but was:<500>`

Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/audit-trail-1`
