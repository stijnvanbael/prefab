# Prefab benchmark: claude-haiku-5-5 with context `full-docs`

| | |
|---|---|
| Run | 20261122-235409-claude-haiku-5-5-full-docs |
| Agent | claude-code/claude-haiku-5-5 |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ 8dc14ab |
| Started | 2026-11-22T23:54:09.670575531Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **9/32 = 28% (95% CI 16-45%)** |
| Tasks passed in every attempt | 9 |
| Cost | $2.18 |
| Tokens (input incl. cache / output) | 15,799,550 / 89,070 |
| Median turns / builds / agent time | 0 / 0 / 0s |
| **Rate limited or skipped** | 23 attempts did not run; the pass rate is not meaningful |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 0 |
| TEST_OR_INFRASTRUCTURE | 23 |

## By task category

| Category | Passed |
|---|---|
| audit | 1/1 |
| avro | 1/2 |
| binary | 1/1 |
| diagnostics | 1/1 |
| events-handling | 3/3 |
| events-publishing | 2/2 |
| lists | 0/5 |
| mongodb | 0/1 |
| multi-tenancy | 0/1 |
| persistence | 0/1 |
| repository-mixins | 0/2 |
| rest-crud | 0/7 |
| security | 0/2 |
| testing | 0/2 |
| views | 0/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 10 | 3 | 1,162,993 / 6,353 | $0.18 | 1m14s |  |
| avro-avsc-contract | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| avro-publish-and-handle | 1 | PASS | +guardrails +hidden-tests | 16 | 2 | 1,418,792 / 7,371 | $0.20 | 1m33s |  |
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 23 | 9 | 3,109,737 / 21,187 | $0.34 | 2m58s |  |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 13 | 4 | 1,297,619 / 5,858 | $0.18 | 1m00s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 14 | 5 | 1,663,706 / 9,494 | $0.22 | 1m45s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 14 | 5 | 1,675,454 / 10,414 | $0.22 | 1m35s |  |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 11 | 2 | 1,153,139 / 6,016 | $0.18 | 55s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 17 | 3 | 1,800,867 / 8,728 | $0.22 | 1m30s |  |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 9 | 3 | 927,776 / 6,797 | $0.16 | 59s |  |
| list-autocomplete | 1 | FAIL | -agent | 13 | 2 | 1,144,307 / 5,136 | $0.17 | 42s | TEST_OR_INFRASTRUCTURE |
| list-filters | 1 | FAIL | -agent | 5 | 2 | 445,160 / 1,716 | $0.12 | 19s | TEST_OR_INFRASTRUCTURE |
| list-full-text-search | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| list-paged-sorted | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| list-range-filter | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| mixin-custom-sql | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| mixin-derived-query | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| mongodb-crud | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| persistence-evolve-schema | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| rest-computed-field | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| rest-create-and-get | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| rest-custom-action | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| rest-parent-child | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| rest-update-and-delete | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| rest-validation | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| rest-value-object | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| security-public-endpoints | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| security-role-based | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| tenancy-isolation | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| testing-domain-events-unit | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| testing-integration-tests | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| view-model | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |

## Failures

### avro-avsc-contract#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/avro-avsc-contract-1`

### list-autocomplete#1

**TEST_OR_INFRASTRUCTURE** (heuristic): rate limited: You've hit your 5-hour limit · resets 4:40am (UTC)


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/list-autocomplete-1`

### list-filters#1

**TEST_OR_INFRASTRUCTURE** (heuristic): rate limited: You've hit your 5-hour limit · resets 4:40am (UTC)


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/list-filters-1`

### list-full-text-search#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/list-full-text-search-1`

### list-paged-sorted#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/list-paged-sorted-1`

### list-range-filter#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/list-range-filter-1`

### mixin-custom-sql#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/mixin-custom-sql-1`

### mixin-derived-query#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/mixin-derived-query-1`

### mongodb-crud#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/mongodb-crud-1`

### persistence-evolve-schema#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/persistence-evolve-schema-1`

### rest-computed-field#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/rest-computed-field-1`

### rest-create-and-get#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/rest-create-and-get-1`

### rest-custom-action#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/rest-custom-action-1`

### rest-parent-child#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/rest-parent-child-1`

### rest-update-and-delete#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/rest-update-and-delete-1`

### rest-validation#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/rest-validation-1`

### rest-value-object#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/rest-value-object-1`

### security-public-endpoints#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/security-public-endpoints-1`

### security-role-based#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/security-role-based-1`

### tenancy-isolation#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/tenancy-isolation-1`

### testing-domain-events-unit#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/testing-domain-events-unit-1`

### testing-integration-tests#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/testing-integration-tests-1`

### view-model#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-235409-claude-haiku-5-5-full-docs/attempts/view-model-1`
