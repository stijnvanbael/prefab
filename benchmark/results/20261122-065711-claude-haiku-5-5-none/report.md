# Prefab benchmark: claude-haiku-5-5 with context `none`

| | |
|---|---|
| Run | 20261122-065711-claude-haiku-5-5-none |
| Agent | claude-code/claude-haiku-5-5 |
| Context | none |
| Prefab | 0.13.3-SNAPSHOT @ be1f9c8 |
| Started | 2026-11-22T06:57:11.671058258Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **30/32 = 94% (95% CI 80-98%)** |
| Tasks passed in every attempt | 30 |
| Cost | $1.65 |
| Tokens (input incl. cache / output) | 42,232,410 / 575,054 |
| Median turns / builds / agent time | 24 / 4 / 1m52s |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 0 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 2 |
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
| mongodb | 0/1 |
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
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 25 | 6 | 980,351 / 14,635 | $0.03 | 2m00s |  |
| avro-avsc-contract | 1 | PASS | +guardrails +hidden-tests | 39 | 6 | 2,460,590 / 28,529 | $0.06 | 3m23s |  |
| avro-publish-and-handle | 1 | PASS | +guardrails +hidden-tests | 26 | 3 | 1,261,361 / 12,773 | $0.03 | 2m30s |  |
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 25 | 5 | 1,287,039 / 19,200 | $0.03 | 2m32s |  |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 14 | 3 | 419,119 / 7,143 | $0.01 | 58s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 22 | 5 | 1,165,301 / 19,699 | $0.03 | 2m08s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 47 | 9 | 3,526,631 / 44,106 | $0.17 | 5m34s |  |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 39 | 2 | 2,171,586 / 20,280 | $0.04 | 2m37s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 54 | 13 | 3,915,649 / 47,635 | $0.26 | 7m39s |  |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 30 | 6 | 1,728,576 / 23,702 | $0.04 | 3m12s |  |
| list-autocomplete | 1 | PASS | +guardrails +hidden-tests | 19 | 2 | 536,158 / 7,849 | $0.01 | 53s |  |
| list-filters | 1 | PASS | +guardrails +hidden-tests | 17 | 4 | 587,517 / 8,641 | $0.02 | 1m19s |  |
| list-full-text-search | 1 | PASS | +guardrails +hidden-tests | 17 | 4 | 807,935 / 14,175 | $0.03 | 1m44s |  |
| list-paged-sorted | 1 | PASS | +guardrails +hidden-tests | 22 | 4 | 809,143 / 10,665 | $0.02 | 1m30s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 15 | 3 | 533,250 / 7,952 | $0.02 | 1m10s |  |
| mixin-custom-sql | 1 | PASS | +guardrails +hidden-tests | 18 | 3 | 607,471 / 8,602 | $0.02 | 1m08s |  |
| mixin-derived-query | 1 | PASS | +guardrails +hidden-tests | 22 | 4 | 586,564 / 8,485 | $0.01 | 1m04s |  |
| mongodb-crud | 1 | FAIL | -guardrails +hidden-tests | 24 | 8 | 1,374,366 / 21,273 | $0.04 | 2m37s | PLAIN_SPRING_FALLBACK |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 27 | 11 | 1,131,411 / 19,404 | $0.03 | 2m25s |  |
| rest-computed-field | 1 | PASS | +guardrails +hidden-tests | 25 | 4 | 897,287 / 11,282 | $0.02 | 1m24s |  |
| rest-create-and-get | 1 | PASS | +guardrails +hidden-tests | 40 | 9 | 2,702,457 / 32,479 | $0.10 | 3m30s |  |
| rest-custom-action | 1 | PASS | +guardrails +hidden-tests | 27 | 4 | 1,134,358 / 10,584 | $0.02 | 1m16s |  |
| rest-parent-child | 1 | FAIL | -guardrails +hidden-tests | 29 | 7 | 1,348,988 / 23,358 | $0.04 | 2m22s | PLAIN_SPRING_FALLBACK |
| rest-update-and-delete | 1 | PASS | +guardrails +hidden-tests | 2 | 5 | 185,421 / 308 | $0.07 | 21m27s |  |
| rest-validation | 1 | PASS | +guardrails +hidden-tests | 18 | 4 | 524,137 / 9,372 | $0.02 | 1m07s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 59 | 29 | 5,117,743 / 74,126 | $0.35 | 8m46s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 28 | 5 | 1,243,005 / 18,206 | $0.03 | 2m14s |  |
| security-role-based | 1 | PASS | +guardrails +hidden-tests | 23 | 6 | 897,060 / 13,277 | $0.02 | 1m27s |  |
| tenancy-isolation | 1 | PASS | +guardrails +hidden-tests | 16 | 3 | 621,126 / 12,118 | $0.02 | 1m13s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 13 | 4 | 438,453 / 8,343 | $0.01 | 55s |  |
| testing-integration-tests | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +agent-tests-pass +mutant-killed:pay-keeps-status +mutant-killed:double-payment-allowed +mutant-killed:amount-not-stored | 15 | 3 | 564,039 / 9,126 | $0.02 | 1m06s |  |
| view-model | 1 | PASS | +guardrails +hidden-tests | 18 | 3 | 668,318 / 7,727 | $0.02 | 1m02s |  |

## Failures

### mongodb-crud#1

**PLAIN_SPRING_FALLBACK** (manual): Hand-wrote ArchiveNoteController and ArchiveNoteService for POST /notes/{id}/archive, believing "Prefab has no custom-action generator" (without docs it did not find @Update(path, method)). Also hit the @Filter-on-boolean processor crash (TASK-351) and worked around it with Boolean. 

- `guardrails: hand-written-controller in src/main/java/com/example/notes/infrastructure/http/ArchiveNoteController.java`
- `guardrail hand-written-controller (FAIL) src/main/java/com/example/notes/infrastructure/http/ArchiveNoteController.java:10`

Logs: `/home/user/prefab/benchmark/results/20261122-065711-claude-haiku-5-5-none/attempts/mongodb-crud-1`

### rest-parent-child#1

**PLAIN_SPRING_FALLBACK** (manual): Hand-wrote get-by-id and complete endpoints because the generated ones ignore {projectId}, so a task was reachable through another project's URL. That is a real Prefab bug (TASK-355); the fallback was a reasonable reaction to it. 

- `guardrails: hand-written-controller in src/main/java/com/example/planning/infrastructure/http/TaskItemController.java`
- `guardrail hand-written-controller (FAIL) src/main/java/com/example/planning/infrastructure/http/TaskItemController.java:18`

Logs: `/home/user/prefab/benchmark/results/20261122-065711-claude-haiku-5-5-none/attempts/rest-parent-child-1`
