# Prefab benchmark: claude-haiku-5-5 with context `full-docs`

| | |
|---|---|
| Run | 20261122-091726-claude-haiku-5-5-full-docs |
| Agent | claude-code/claude-haiku-5-5 |
| Context | full-docs |
| Prefab | 0.13.3-SNAPSHOT @ fa2b6e9 |
| Started | 2026-11-22T09:17:26.761558326Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **0/32 = 0% (95% CI 0-11%)** |
| Tasks passed in every attempt | 0 |
| Cost | $0.46 |
| Tokens (input incl. cache / output) | 5,314,694 / 27,263 |
| Median turns / builds / agent time | 0 / 0 / 0s |
| **Rate limited or skipped** | 32 attempts did not run; the pass rate is not meaningful |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 0 |
| TEST_OR_INFRASTRUCTURE | 32 |

## By task category

| Category | Passed |
|---|---|
| audit | 0/1 |
| avro | 0/2 |
| binary | 0/1 |
| diagnostics | 0/1 |
| events-handling | 0/3 |
| events-publishing | 0/2 |
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
| audit-trail | 1 | FAIL | -agent | 23 | 5 | 2,457,980 / 13,652 | $0.21 | 1m44s | TEST_OR_INFRASTRUCTURE |
| avro-avsc-contract | 1 | FAIL | -agent | 26 | 6 | 2,856,714 / 13,611 | $0.25 | 2m44s | TEST_OR_INFRASTRUCTURE |
| avro-publish-and-handle | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| binary-upload-download | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| diagnostic-fix-build | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| events-create-from-event | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| events-multicast | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| events-publish-on-create | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| events-sealed-hierarchy | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
| events-update-by-reference | 1 | FAIL | -agent | 0 | 0 | 0 / 0 | $0.00 | 0s | TEST_OR_INFRASTRUCTURE |
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

### audit-trail#1

**TEST_OR_INFRASTRUCTURE** (heuristic): rate limited: You've hit your 5-hour limit · resets 11:10am (UTC)


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/audit-trail-1`

### avro-avsc-contract#1

**TEST_OR_INFRASTRUCTURE** (heuristic): rate limited: You've hit your 5-hour limit · resets 11:10am (UTC)


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/avro-avsc-contract-1`

### avro-publish-and-handle#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/avro-publish-and-handle-1`

### binary-upload-download#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/binary-upload-download-1`

### diagnostic-fix-build#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/diagnostic-fix-build-1`

### events-create-from-event#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/events-create-from-event-1`

### events-multicast#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/events-multicast-1`

### events-publish-on-create#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/events-publish-on-create-1`

### events-sealed-hierarchy#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/events-sealed-hierarchy-1`

### events-update-by-reference#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/events-update-by-reference-1`

### list-autocomplete#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/list-autocomplete-1`

### list-filters#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/list-filters-1`

### list-full-text-search#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/list-full-text-search-1`

### list-paged-sorted#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/list-paged-sorted-1`

### list-range-filter#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/list-range-filter-1`

### mixin-custom-sql#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/mixin-custom-sql-1`

### mixin-derived-query#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/mixin-derived-query-1`

### mongodb-crud#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/mongodb-crud-1`

### persistence-evolve-schema#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/persistence-evolve-schema-1`

### rest-computed-field#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/rest-computed-field-1`

### rest-create-and-get#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/rest-create-and-get-1`

### rest-custom-action#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/rest-custom-action-1`

### rest-parent-child#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/rest-parent-child-1`

### rest-update-and-delete#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/rest-update-and-delete-1`

### rest-validation#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/rest-validation-1`

### rest-value-object#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/rest-value-object-1`

### security-public-endpoints#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/security-public-endpoints-1`

### security-role-based#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/security-role-based-1`

### tenancy-isolation#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/tenancy-isolation-1`

### testing-domain-events-unit#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/testing-domain-events-unit-1`

### testing-integration-tests#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/testing-integration-tests-1`

### view-model#1

**TEST_OR_INFRASTRUCTURE** (heuristic): not started: an earlier attempt hit a usage or rate limit


Logs: `benchmark/results/20261122-091726-claude-haiku-5-5-full-docs/attempts/view-model-1`
