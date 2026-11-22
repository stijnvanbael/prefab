# Prefab benchmark: claude-sonnet-5-5 with context `full-docs`

| | |
|---|---|
| Run | 20261122-195459-claude-sonnet-5-5-full-docs |
| Agent | claude-code/claude-sonnet-5-5 |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ eab5431 |
| Started | 2026-11-22T19:54:59.905434220Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **32/32 = 100% (95% CI 89-100%)** |
| Tasks passed in every attempt | 32 |
| Cost | $15.08 |
| Tokens (input incl. cache / output) | 20,994,306 / 62,178 |
| Median turns / builds / agent time | 6 / 2 / 27s |

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
| rest-crud | 7/7 |
| security | 2/2 |
| testing | 2/2 |
| views | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 5 | 1 | 549,701 / 1,569 | $0.45 | 20s |  |
| avro-avsc-contract | 1 | PASS | +guardrails +hidden-tests | 8 | 3 | 905,401 / 2,808 | $0.52 | 1m36s |  |
| avro-publish-and-handle | 1 | PASS | +guardrails +hidden-tests | 8 | 3 | 894,137 / 2,555 | $0.50 | 1m07s |  |
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 17 | 5 | 1,889,455 / 5,784 | $0.68 | 2m04s |  |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 7 | 3 | 783,640 / 1,807 | $0.49 | 52s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 7 | 1 | 772,539 / 1,550 | $0.47 | 23s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 3 | 1 | 327,973 / 1,121 | $0.42 | 15s |  |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 4 | 1 | 437,768 / 1,253 | $0.43 | 16s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 5 | 2 | 552,962 / 1,794 | $0.46 | 37s |  |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 3 | 1 | 328,459 / 1,067 | $0.42 | 27s |  |
| list-autocomplete | 1 | PASS | +guardrails +hidden-tests | 4 | 2 | 439,261 / 866 | $0.43 | 21s |  |
| list-filters | 1 | PASS | +guardrails +hidden-tests | 4 | 2 | 438,192 / 1,071 | $0.43 | 21s |  |
| list-full-text-search | 1 | PASS | +guardrails +hidden-tests | 5 | 2 | 550,834 / 1,718 | $0.45 | 27s |  |
| list-paged-sorted | 1 | PASS | +guardrails +hidden-tests | 4 | 1 | 435,765 / 693 | $0.42 | 13s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 7 | 2 | 781,861 / 1,990 | $0.49 | 26s |  |
| mixin-custom-sql | 1 | PASS | +guardrails +hidden-tests | 7 | 2 | 772,887 / 2,029 | $0.49 | 38s |  |
| mixin-derived-query | 1 | PASS | +guardrails +hidden-tests | 3 | 1 | 327,099 / 712 | $0.41 | 12s |  |
| mongodb-crud | 1 | PASS | +guardrails +hidden-tests | 6 | 2 | 673,449 / 3,164 | $0.51 | 50s |  |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 6 | 4 | 661,141 / 1,687 | $0.46 | 33s |  |
| rest-computed-field | 1 | PASS | +guardrails +hidden-tests | 3 | 1 | 327,448 / 726 | $0.41 | 14s |  |
| rest-create-and-get | 1 | PASS | +guardrails +hidden-tests | 10 | 4 | 1,015,917 / 2,852 | $0.53 | 1m17s |  |
| rest-custom-action | 1 | PASS | +guardrails +hidden-tests | 5 | 1 | 547,748 / 1,337 | $0.44 | 19s |  |
| rest-parent-child | 1 | PASS | +guardrails +hidden-tests | 6 | 3 | 668,377 / 2,803 | $0.49 | 47s |  |
| rest-update-and-delete | 1 | PASS | +guardrails +hidden-tests | 3 | 1 | 326,857 / 706 | $0.41 | 12s |  |
| rest-validation | 1 | PASS | +guardrails +hidden-tests | 9 | 5 | 1,000,237 / 2,139 | $0.51 | 1m05s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 7 | 2 | 780,385 / 2,232 | $0.49 | 39s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 8 | 3 | 913,059 / 3,514 | $0.55 | 1m01s |  |
| security-role-based | 1 | PASS | +guardrails +hidden-tests | 3 | 1 | 328,721 / 1,041 | $0.42 | 14s |  |
| tenancy-isolation | 1 | PASS | +guardrails +hidden-tests | 7 | 3 | 783,014 / 3,541 | $0.51 | 57s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 4 | 1 | 442,319 / 1,725 | $0.44 | 18s |  |
| testing-integration-tests | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +agent-tests-pass +mutant-killed:pay-keeps-status +mutant-killed:double-payment-allowed +mutant-killed:amount-not-stored | 6 | 3 | 676,737 / 2,683 | $0.50 | 57s |  |
| view-model | 1 | PASS | +guardrails +hidden-tests | 6 | 2 | 660,963 / 1,641 | $0.46 | 27s |  |
