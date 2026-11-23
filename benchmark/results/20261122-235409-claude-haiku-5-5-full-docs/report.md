# Prefab benchmark: claude-haiku-5-5 with context `full-docs`

| | |
|---|---|
| Run | 20261122-235409-claude-haiku-5-5-full-docs |
| Agent | claude-code/claude-haiku-5-5 |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ 8dc14ab |
| Started | 2026-11-22T23:54:09.670575531Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **31/32 = 97% (95% CI 84-99%)** |
| Tasks passed in every attempt | 31 |
| Cost | $6.65 |
| Tokens (input incl. cache / output) | 51,060,292 / 268,645 |
| Median turns / builds / agent time | 13 / 4 / 1m10s |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 1 |
| TEST_OR_INFRASTRUCTURE | 0 |

## By task category

| Category | Passed |
|---|---|
| audit | 1/1 |
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
| rest-crud | 6/7 |
| security | 2/2 |
| testing | 2/2 |
| views | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 10 | 3 | 1,162,993 / 6,353 | $0.18 | 1m14s |  |
| avro-avsc-contract | 1 | PASS | +guardrails +hidden-tests | 15 | 5 | 1,816,530 / 9,829 | $0.23 | 3m07s |  |
| avro-publish-and-handle | 1 | PASS | +guardrails +hidden-tests | 16 | 2 | 1,418,792 / 7,371 | $0.20 | 1m33s |  |
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 23 | 9 | 3,109,737 / 21,187 | $0.34 | 2m58s |  |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 13 | 4 | 1,297,619 / 5,858 | $0.18 | 1m00s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 14 | 5 | 1,663,706 / 9,494 | $0.22 | 1m45s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 14 | 5 | 1,675,454 / 10,414 | $0.22 | 1m35s |  |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 11 | 2 | 1,153,139 / 6,016 | $0.18 | 55s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 17 | 3 | 1,800,867 / 8,728 | $0.22 | 1m30s |  |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 9 | 3 | 927,776 / 6,797 | $0.16 | 59s |  |
| list-autocomplete | 1 | PASS | +guardrails +hidden-tests | 13 | 2 | 1,376,611 / 5,363 | $0.18 | 46s |  |
| list-filters | 1 | PASS | +guardrails +hidden-tests | 15 | 6 | 1,674,121 / 8,049 | $0.22 | 2m03s |  |
| list-full-text-search | 1 | PASS | +guardrails +hidden-tests | 15 | 6 | 1,803,732 / 9,826 | $0.22 | 1m43s |  |
| list-paged-sorted | 1 | PASS | +guardrails +hidden-tests | 15 | 4 | 1,773,642 / 9,864 | $0.22 | 1m22s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 18 | 5 | 2,135,170 / 8,222 | $0.23 | 1m17s |  |
| mixin-custom-sql | 1 | PASS | +guardrails +hidden-tests | 10 | 4 | 1,165,445 / 4,771 | $0.17 | 58s |  |
| mixin-derived-query | 1 | PASS | +guardrails +hidden-tests | 11 | 2 | 1,138,456 / 4,849 | $0.17 | 44s |  |
| mongodb-crud | 1 | PASS | +guardrails +hidden-tests | 23 | 7 | 2,283,852 / 11,027 | $0.25 | 1m32s |  |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 13 | 4 | 1,307,363 / 6,826 | $0.19 | 1m00s |  |
| rest-computed-field | 1 | PASS | +guardrails +hidden-tests | 10 | 2 | 1,137,915 / 4,094 | $0.16 | 34s |  |
| rest-create-and-get | 1 | PASS | +guardrails +hidden-tests | 11 | 3 | 1,277,224 / 6,849 | $0.18 | 1m02s |  |
| rest-custom-action | 1 | PASS | +guardrails +hidden-tests | 11 | 4 | 1,268,946 / 5,560 | $0.18 | 51s |  |
| rest-parent-child | 1 | FAIL | -guardrails +hidden-tests | 17 | 8 | 1,942,570 / 14,172 | $0.24 | 1m46s | PLAIN_SPRING_FALLBACK |
| rest-update-and-delete | 1 | PASS | +guardrails +hidden-tests | 9 | 2 | 1,042,153 / 5,189 | $0.17 | 40s |  |
| rest-validation | 1 | PASS | +guardrails +hidden-tests | 20 | 8 | 2,383,731 / 9,282 | $0.25 | 1m28s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 12 | 4 | 1,408,468 / 7,029 | $0.19 | 1m07s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 24 | 9 | 3,177,237 / 20,497 | $0.35 | 2m52s |  |
| security-role-based | 1 | PASS | +guardrails +hidden-tests | 17 | 3 | 2,055,071 / 10,983 | $0.24 | 1m25s |  |
| tenancy-isolation | 1 | PASS | +guardrails +hidden-tests | 10 | 3 | 1,175,774 / 8,362 | $0.19 | 1m05s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 6 | 1 | 682,683 / 4,088 | $0.14 | 25s |  |
| testing-integration-tests | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +agent-tests-pass +mutant-killed:pay-keeps-status +mutant-killed:double-payment-allowed +mutant-killed:amount-not-stored | 11 | 3 | 1,311,322 / 6,238 | $0.19 | 1m01s |  |
| view-model | 1 | PASS | +guardrails +hidden-tests | 13 | 3 | 1,512,193 / 5,458 | $0.19 | 57s |  |

## Failures

### rest-parent-child#1

**PLAIN_SPRING_FALLBACK** (manual): Made Task Exposure.INTERNAL and hand-wrote the controller, because the generated get/complete endpoints ignore {projectId} (TASK-355). Third run where an agent works around this bug. 

- `guardrails: generated-class-override in src/main/java/com/example/planning/infrastructure/http/TaskController.java; hand-written-controller in src/main/java/com/example/planning/infrastructure/http/TaskController.java`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/planning/infrastructure/http/TaskController.java`
- `guardrail hand-written-controller (FAIL) src/main/java/com/example/planning/infrastructure/http/TaskController.java:23`

Logs: `benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/rest-parent-child-1`
