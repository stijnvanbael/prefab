# Prefab benchmark: claude-sonnet-5-5 with context `full-docs`

| | |
|---|---|
| Run | 20261122-195459-claude-sonnet-5-5-full-docs |
| Agent | claude-code/claude-sonnet-5-5 |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ eab5431 |
| Started | 2026-11-22T19:54:59.905434220Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **8/32 = 25% (95% CI 13-42%)** |
| Tasks passed in every attempt | 8 |
| Cost | $4.74 |
| Tokens (input incl. cache / output) | 6,886,775 / 19,701 |
| Median turns / builds / agent time | 0 / 0 / 0s |
| **Rate limited or skipped** | 24 attempts did not run; the pass rate is not meaningful |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 0 |
| TEST_OR_INFRASTRUCTURE | 24 |

## By task category

| Category | Passed |
|---|---|
| audit | 1/1 |
| avro | 2/2 |
| binary | 1/1 |
| diagnostics | 1/1 |
| events-handling | 2/3 |
| events-publishing | 1/2 |
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
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 5 | 1 | 549,701 / 1,569 | $0.45 | 20s |  |
| avro-avsc-contract | 1 | PASS | +guardrails +hidden-tests | 8 | 3 | 905,401 / 2,808 | $0.52 | 1m36s |  |
| avro-publish-and-handle | 1 | PASS | +guardrails +hidden-tests | 8 | 3 | 894,137 / 2,555 | $0.50 | 1m07s |  |
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 17 | 5 | 1,889,455 / 5,784 | $0.68 | 2m04s |  |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 7 | 3 | 783,640 / 1,807 | $0.49 | 52s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 7 | 1 | 772,539 / 1,550 | $0.47 | 23s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 3 | 1 | 327,973 / 1,121 | $0.42 | 15s |  |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 4 | 1 | 437,768 / 1,253 | $0.43 | 16s |  |
| events-sealed-hierarchy | 1 | FAIL | -agent | 3 | 1 | 217,810 / 1,092 | $0.40 | 12s | TEST_OR_INFRASTRUCTURE |
| events-update-by-reference | 1 | FAIL | -agent | 2 | 0 | 108,351 / 162 | $0.38 | 3s | TEST_OR_INFRASTRUCTURE |
| list-autocomplete | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| list-filters | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
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

### events-sealed-hierarchy#1

**TEST_OR_INFRASTRUCTURE** (heuristic): rate limited: You've hit your 5-hour limit · resets 11:40pm (UTC)


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/events-sealed-hierarchy-1`

### events-update-by-reference#1

**TEST_OR_INFRASTRUCTURE** (heuristic): rate limited: You've hit your 5-hour limit · resets 11:40pm (UTC)


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/events-update-by-reference-1`

### list-autocomplete#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/list-autocomplete-1`

### list-filters#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/list-filters-1`

### list-full-text-search#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/list-full-text-search-1`

### list-paged-sorted#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/list-paged-sorted-1`

### list-range-filter#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/list-range-filter-1`

### mixin-custom-sql#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/mixin-custom-sql-1`

### mixin-derived-query#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/mixin-derived-query-1`

### mongodb-crud#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/mongodb-crud-1`

### persistence-evolve-schema#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/persistence-evolve-schema-1`

### rest-computed-field#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/rest-computed-field-1`

### rest-create-and-get#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/rest-create-and-get-1`

### rest-custom-action#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/rest-custom-action-1`

### rest-parent-child#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/rest-parent-child-1`

### rest-update-and-delete#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/rest-update-and-delete-1`

### rest-validation#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/rest-validation-1`

### rest-value-object#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/rest-value-object-1`

### security-public-endpoints#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/security-public-endpoints-1`

### security-role-based#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/security-role-based-1`

### tenancy-isolation#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/tenancy-isolation-1`

### testing-domain-events-unit#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/testing-domain-events-unit-1`

### testing-integration-tests#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/testing-integration-tests-1`

### view-model#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `/home/user/prefab/benchmark/results/20261122-195459-claude-sonnet-5-5-full-docs/attempts/view-model-1`
