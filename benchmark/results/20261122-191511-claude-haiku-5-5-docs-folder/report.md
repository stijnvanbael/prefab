# Prefab benchmark: claude-haiku-5-5 with context `docs-folder`

| | |
|---|---|
| Run | 20261122-191511-claude-haiku-5-5-docs-folder |
| Agent | claude-code/claude-haiku-5-5 |
| Context | docs-folder |
| Prefab | 0.13.3-SNAPSHOT @ 647813a |
| Started | 2026-11-22T19:15:11.157564934Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **29/32 = 91% (95% CI 76-97%)** |
| Tasks passed in every attempt | 29 |
| Cost | $0.77 |
| Tokens (input incl. cache / output) | 28,145,041 / 375,814 |
| Median turns / builds / agent time | 18 / 4 / 1m22s |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 1 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 1 |
| TEST_OR_INFRASTRUCTURE | 1 |

## By task category

| Category | Passed |
|---|---|
| audit | 1/1 |
| avro | 2/2 |
| binary | 1/1 |
| diagnostics | 1/1 |
| events-handling | 3/3 |
| events-publishing | 1/2 |
| lists | 5/5 |
| mongodb | 1/1 |
| multi-tenancy | 1/1 |
| persistence | 1/1 |
| repository-mixins | 1/2 |
| rest-crud | 6/7 |
| security | 2/2 |
| testing | 2/2 |
| views | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 17 | 4 | 787,394 / 12,127 | $0.02 | 1m23s |  |
| avro-avsc-contract | 1 | PASS | +guardrails +hidden-tests | 33 | 6 | 1,893,514 / 19,605 | $0.06 | 2m33s |  |
| avro-publish-and-handle | 1 | PASS | +guardrails +hidden-tests | 29 | 5 | 1,272,198 / 15,300 | $0.03 | 1m56s |  |
| binary-upload-download | 1 | PASS | +guardrails +hidden-tests | 36 | 7 | 1,906,331 / 30,401 | $0.05 | 3m16s |  |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 17 | 4 | 675,493 / 8,031 | $0.02 | 1m13s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 24 | 4 | 1,086,089 / 13,351 | $0.03 | 1m36s |  |
| events-multicast | 1 | PASS | +guardrails +hidden-tests | 18 | 3 | 697,041 / 9,400 | $0.02 | 1m13s |  |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 24 | 4 | 1,210,627 / 13,035 | $0.03 | 1m50s |  |
| events-sealed-hierarchy | 1 | FAIL | +guardrails -hidden-tests | 23 | 5 | 827,274 / 10,974 | $0.02 | 1m26s | DOC_GAP |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 15 | 3 | 624,856 / 8,002 | $0.02 | 1m22s |  |
| list-autocomplete | 1 | PASS | +guardrails +hidden-tests | 15 | 2 | 550,653 / 6,333 | $0.02 | 48s |  |
| list-filters | 1 | PASS | +guardrails +hidden-tests | 14 | 3 | 560,171 / 9,906 | $0.02 | 1m10s |  |
| list-full-text-search | 1 | PASS | +guardrails +hidden-tests | 15 | 6 | 682,237 / 11,344 | $0.02 | 1m45s |  |
| list-paged-sorted | 1 | PASS | +guardrails +hidden-tests | 15 | 2 | 542,369 / 8,092 | $0.02 | 1m01s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 15 | 3 | 613,183 / 10,380 | $0.02 | 1m11s |  |
| mixin-custom-sql | 1 | FAIL | +guardrails -hidden-tests | 13 | 3 | 447,248 / 7,252 | $0.01 | 55s | TEST_OR_INFRASTRUCTURE |
| mixin-derived-query | 1 | PASS | +guardrails +hidden-tests | 14 | 2 | 487,685 / 5,907 | $0.01 | 45s |  |
| mongodb-crud | 1 | PASS | +guardrails +hidden-tests | 24 | 5 | 1,251,989 / 13,191 | $0.03 | 1m32s |  |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 18 | 7 | 735,941 / 9,710 | $0.02 | 1m30s |  |
| rest-computed-field | 1 | PASS | +guardrails +hidden-tests | 15 | 2 | 469,860 / 6,425 | $0.01 | 47s |  |
| rest-create-and-get | 1 | PASS | +guardrails +hidden-tests | 13 | 2 | 477,345 / 8,074 | $0.02 | 56s |  |
| rest-custom-action | 1 | PASS | +guardrails +hidden-tests | 18 | 2 | 678,070 / 7,827 | $0.02 | 54s |  |
| rest-parent-child | 1 | FAIL | -guardrails +hidden-tests | 32 | 4 | 1,701,622 / 20,836 | $0.04 | 2m06s | PLAIN_SPRING_FALLBACK |
| rest-update-and-delete | 1 | PASS | +guardrails +hidden-tests | 18 | 3 | 692,654 / 8,907 | $0.02 | 1m11s |  |
| rest-validation | 1 | PASS | +guardrails +hidden-tests | 26 | 5 | 1,120,249 / 11,675 | $0.03 | 14m08s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 22 | 6 | 790,693 / 9,839 | $0.02 | 1m20s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 29 | 9 | 1,513,913 / 22,542 | $0.04 | 2m54s |  |
| security-role-based | 1 | PASS | +guardrails +hidden-tests | 21 | 6 | 947,316 / 13,902 | $0.03 | 1m29s |  |
| tenancy-isolation | 1 | PASS | +guardrails +hidden-tests | 21 | 3 | 853,966 / 15,133 | $0.03 | 1m39s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 8 | 3 | 248,722 / 5,621 | $0.01 | 36s |  |
| testing-integration-tests | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +agent-tests-pass +mutant-killed:pay-keeps-status +mutant-killed:double-payment-allowed +mutant-killed:amount-not-stored | 21 | 3 | 869,988 / 11,470 | $0.02 | 1m19s |  |
| view-model | 1 | PASS | +guardrails +hidden-tests | 22 | 4 | 928,350 / 11,222 | $0.02 | 1m29s |  |

## Failures

### events-sealed-hierarchy#1

**DOC_GAP** (manual): Sealed JSON event interface without @JsonTypeInfo(use = NAME): consumers of AccountEvent cannot deserialize the events (InvalidDefinitionException). The docs do not mention the annotation (TASK-354). The agent's own test only checked the publishing side. 

- `hidden-tests: 0/1 passed; com.example.banking.AccountEventsAcceptanceTest.depositsAndWithdrawalsArePublishedInOrder: Expected size: 2 but was: 0 in: []`
- `build: 2026-11-22T19:24:20.173Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.179Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.180Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.181Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.182Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.185Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.186Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.187Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.188Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`
- `build: 2026-11-22T19:24:20.189Z ERROR 6770 --- [events-sealed-hierarchy] [     virtual-39] o.a.k.c.c.internals.CompletedFetch       : [Consumer clientId=consumer-AccountEventsAcceptanceTest-0-1, groupId=AccountEventsAcceptanceTest-0] Value Deserializers with error: Deserializers{keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer@1a0f112c, valueDeserializer=be.appify.prefab.co...`

Logs: `/home/user/prefab/benchmark/results/20261122-191511-claude-haiku-5-5-docs-folder/attempts/events-sealed-hierarchy-1`

### mixin-custom-sql#1

**TEST_OR_INFRASTRUCTURE** (manual): Connection refused to Postgres during the hidden tests: the acceptance build reused another parallel slot's Postgres container (Testcontainers reuse matches on a configuration hash that ignores the container name), which that slot then removed. Fixed by disabling container reuse in the harness; prefab-test issue filed as TASK-356. 

- `hidden-tests: 0/2 passed; com.example.ordering.OrderRevenueAcceptanceTest.revenueIsZeroWithoutOrders: Failed to obtain JDBC Connection | com.example.ordering.OrderRevenueAcceptanceTest.sumsTotalsPerStatus: Status expected:<201> but was:<500>`

Logs: `/home/user/prefab/benchmark/results/20261122-191511-claude-haiku-5-5-docs-folder/attempts/mixin-custom-sql-1`

### rest-parent-child#1

**PLAIN_SPRING_FALLBACK** (manual): Same as the no-docs run: hand-wrote the sub-resource endpoints because the generated get/complete endpoints ignore {projectId} (TASK-355). 

- `guardrails: hand-written-controller in src/main/java/com/example/planning/infrastructure/http/TaskSubResourceController.java`
- `guardrail hand-written-controller (FAIL) src/main/java/com/example/planning/infrastructure/http/TaskSubResourceController.java:17`

Logs: `/home/user/prefab/benchmark/results/20261122-191511-claude-haiku-5-5-docs-folder/attempts/rest-parent-child-1`
