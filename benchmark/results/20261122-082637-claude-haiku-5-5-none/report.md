# Prefab benchmark: claude-haiku-5-5 with context `none`

| | |
|---|---|
| Run | 20261122-082637-claude-haiku-5-5-none |
| Agent | claude-code/claude-haiku-5-5 |
| Context | none |
| Prefab | 0.13.3-SNAPSHOT @ d97bc22 |
| Started | 2026-11-22T08:26:37.951248048Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **30/32 = 94% (95% CI 80-98%)** |
| Tasks passed in every attempt | 30 |
| Cost | $2.12 |
| Tokens (input incl. cache / output) | 54,138,368 / 629,772 |
| Median turns / builds / agent time | 24 / 6 / 1m51s |

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
| events-handling | 2/3 |
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
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 19 | 4 | 874,779 / 15,324 | $0.03 | 1m41s |  |
| avro-avsc-contract | 1 | PASS | +guardrails +hidden-tests | 21 | 5 | 969,778 / 15,183 | $0.03 | 2m09s |  |
| avro-publish-and-handle | 1 | PASS | +guardrails +hidden-tests | 47 | 11 | 3,447,958 / 42,640 | $0.18 | 5m06s |  |
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 48 | 11 | 3,942,707 / 48,773 | $0.21 | 5m13s |  |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 17 | 3 | 525,748 / 7,368 | $0.02 | 1m02s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 30 | 8 | 1,739,808 / 20,876 | $0.04 | 2m29s |  |
| events-multicast | 1 | FAIL | -guardrails +hidden-tests | 38 | 9 | 2,392,770 / 39,240 | $0.11 | 5m45s | PLAIN_SPRING_FALLBACK |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 25 | 3 | 1,267,285 / 18,046 | $0.03 | 1m56s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 40 | 7 | 2,575,157 / 28,621 | $0.08 | 5m18s |  |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 48 | 7 | 3,026,805 / 34,792 | $0.10 | 3m44s |  |
| list-autocomplete | 1 | PASS | +guardrails +hidden-tests | 18 | 2 | 462,270 / 7,782 | $0.01 | 49s |  |
| list-filters | 1 | PASS | +guardrails +hidden-tests | 23 | 7 | 905,882 / 12,594 | $0.02 | 2m12s |  |
| list-full-text-search | 1 | PASS | +guardrails +hidden-tests | 27 | 4 | 1,560,776 / 22,019 | $0.04 | 2m20s |  |
| list-paged-sorted | 1 | PASS | +guardrails +hidden-tests | 12 | 3 | 369,077 / 8,017 | $0.01 | 1m09s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 17 | 3 | 608,880 / 11,260 | $0.02 | 1m23s |  |
| mixin-custom-sql | 1 | PASS | +guardrails +hidden-tests | 24 | 6 | 880,876 / 14,172 | $0.02 | 1m51s |  |
| mixin-derived-query | 1 | PASS | +guardrails +hidden-tests | 27 | 6 | 771,553 / 12,063 | $0.02 | 1m40s |  |
| mongodb-crud | 1 | PASS | +guardrails +hidden-tests | 34 | 13 | 1,986,700 / 25,797 | $0.05 | 3m11s |  |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 17 | 6 | 658,773 / 10,521 | $0.02 | 1m35s |  |
| rest-computed-field | 1 | PASS | +guardrails +hidden-tests | 19 | 3 | 592,890 / 8,176 | $0.01 | 1m03s |  |
| rest-create-and-get | 1 | FAIL | -guardrails +hidden-tests | 124 | 13 | 13,118,313 / 57,980 | $0.66 | 11m59s | PLAIN_SPRING_FALLBACK |
| rest-custom-action | 1 | PASS | +guardrails +hidden-tests | 24 | 6 | 1,114,965 / 12,684 | $0.03 | 1m34s |  |
| rest-parent-child | 1 | PASS | +guardrails +hidden-tests | 22 | 5 | 966,316 / 15,356 | $0.03 | 1m39s |  |
| rest-update-and-delete | 1 | PASS | +guardrails +hidden-tests | 19 | 6 | 766,034 / 12,607 | $0.02 | 1m32s |  |
| rest-validation | 1 | PASS | +guardrails +hidden-tests | 15 | 4 | 456,829 / 8,629 | $0.01 | 1m05s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 40 | 11 | 3,055,286 / 37,398 | $0.17 | 4m10s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 22 | 3 | 935,761 / 15,136 | $0.03 | 1m51s |  |
| security-role-based | 1 | PASS | +guardrails +hidden-tests | 25 | 5 | 1,094,982 / 15,485 | $0.03 | 1m57s |  |
| tenancy-isolation | 1 | PASS | +guardrails +hidden-tests | 26 | 7 | 1,363,233 / 21,712 | $0.04 | 2m47s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 11 | 3 | 308,061 / 7,385 | $0.01 | 48s |  |
| testing-integration-tests | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +agent-tests-pass +mutant-killed:pay-keeps-status +mutant-killed:double-payment-allowed +mutant-killed:amount-not-stored | 15 | 3 | 488,291 / 10,493 | $0.02 | 1m19s |  |
| view-model | 1 | PASS | +guardrails +hidden-tests | 22 | 4 | 909,825 / 11,643 | $0.02 | 1m28s |  |

## Failures

### events-multicast#1

**PLAIN_SPRING_FALLBACK** (manual): Without docs it did not find @Multicast/@RepositoryMixin: it replaced the generated PriceWatchRepository with a hand-written one (findAllByProduct) and handled PriceChanged in a hand-written @KafkaListener. The listener was not caught by the guardrails yet; the hand-written-consumer rule was added after this run. 

- `guardrails: generated-class-override in src/main/java/com/example/watch/application/PriceWatchRepository.java; spring-data-repository in src/main/java/com/example/watch/application/PriceWatchRepository.java`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/watch/application/PriceWatchRepository.java`
- `guardrail spring-data-repository (FAIL) src/main/java/com/example/watch/application/PriceWatchRepository.java:10`

Logs: `/home/user/prefab/benchmark/results/20261122-082637-claude-haiku-5-5-none/attempts/events-multicast-1`

### rest-create-and-get#1

**PLAIN_SPRING_FALLBACK** (manual): Overrode the generated BookResponse to leave out the version field, which the prompt does not ask for (the response may contain more fields). Not wrong behaviour, but replacing a generated class. 

- `guardrails: generated-class-override in src/main/java/com/example/catalog/infrastructure/http/BookResponse.java`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/catalog/infrastructure/http/BookResponse.java`

Logs: `/home/user/prefab/benchmark/results/20261122-082637-claude-haiku-5-5-none/attempts/rest-create-and-get-1`
