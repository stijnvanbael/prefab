# Prefab benchmark: claude-haiku-5-5 with context `none`

| | |
|---|---|
| Run | 20261124-111212-claude-haiku-5-5-none |
| Agent | claude-code/claude-haiku-5-5 |
| Context | none |
| Prefab | 0.13.3-SNAPSHOT @ e2824ba-dirty |
| Started | 2026-11-24T11:12:12.192020135Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **21/32 = 66% (95% CI 48-80%)** |
| Tasks passed in every attempt | 21 |
| Cost | $1.08 |
| Tokens (input incl. cache / output) | 38,755,660 / 533,851 |
| Median turns / builds / agent time | 25 / 5 / 2m00s |
| **Contaminated attempts** | avro-publish-and-handle#1 |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 4 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 4 |
| TEST_OR_INFRASTRUCTURE | 3 |

## By task category

| Category | Passed |
|---|---|
| audit | 1/1 |
| avro | 0/2 |
| binary | 0/1 |
| diagnostics | 1/1 |
| events-handling | 2/3 |
| events-publishing | 1/2 |
| lists | 4/5 |
| mongodb | 0/1 |
| multi-tenancy | 1/1 |
| persistence | 1/1 |
| repository-mixins | 1/2 |
| rest-crud | 5/7 |
| security | 2/2 |
| testing | 1/2 |
| views | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 21 | 4 | 859,210 / 13,463 | $0.02 | 1m46s |  |
| avro-avsc-contract | 1 | FAIL | +guardrails -hidden-tests | 32 | 5 | 2,070,655 / 22,191 | $0.06 | 2m57s | DOC_GAP |
| avro-publish-and-handle | 1 | FAIL | +guardrails -hidden-tests | 38 | 7 | 2,374,743 / 25,162 | $0.06 | 3m00s | TEST_OR_INFRASTRUCTURE |
| binary-upload-download | 1 | FAIL | +guardrails -hidden-tests | 34 | 6 | 1,994,667 / 21,016 | $0.04 | 2m53s | DOC_GAP |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 15 | 3 | 455,174 / 6,416 | $0.01 | 1m13s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 26 | 7 | 1,244,155 / 19,264 | $0.04 | 2m24s |  |
| events-multicast | 1 | FAIL | -guardrails +hidden-tests | 39 | 10 | 2,470,401 / 35,730 | $0.09 | 4m40s | PLAIN_SPRING_FALLBACK |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 34 | 8 | 1,735,049 / 25,664 | $0.05 | 4m28s |  |
| events-sealed-hierarchy | 1 | FAIL | +guardrails -hidden-tests | 17 | 4 | 646,849 / 9,871 | $0.02 | 1m21s | DOC_GAP |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 48 | 12 | 3,138,163 / 37,054 | $0.13 | 6m45s |  |
| list-autocomplete | 1 | PASS | +guardrails +hidden-tests | 22 | 4 | 688,154 / 8,362 | $0.02 | 1m21s |  |
| list-filters | 1 | PASS | +guardrails +hidden-tests | 18 | 5 | 726,061 / 14,420 | $0.02 | 1m46s |  |
| list-full-text-search | 1 | FAIL | -guardrails +hidden-tests | 41 | 21 | 2,575,752 / 37,415 | $0.06 | 6m28s | PLAIN_SPRING_FALLBACK |
| list-paged-sorted | 1 | PASS | +guardrails +hidden-tests | 18 | 3 | 601,619 / 10,565 | $0.02 | 1m15s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 16 | 2 | 532,989 / 8,902 | $0.02 | 1m07s |  |
| mixin-custom-sql | 1 | FAIL | +guardrails -hidden-tests | 35 | 6 | 1,471,101 / 13,365 | $0.03 | 2m37s | TEST_OR_INFRASTRUCTURE |
| mixin-derived-query | 1 | PASS | +guardrails +hidden-tests | 28 | 4 | 1,144,289 / 14,549 | $0.03 | 2m01s |  |
| mongodb-crud | 1 | FAIL | +guardrails -hidden-tests | 27 | 7 | 1,565,417 / 16,496 | $0.04 | 2m03s | DOC_GAP |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 31 | 8 | 1,207,167 / 17,212 | $0.03 | 2m32s |  |
| rest-computed-field | 1 | FAIL | -guardrails +hidden-tests | 23 | 4 | 1,014,856 / 21,384 | $0.03 | 2m13s | PLAIN_SPRING_FALLBACK |
| rest-create-and-get | 1 | PASS | +guardrails +hidden-tests | 20 | 2 | 982,608 / 11,639 | $0.03 | 1m26s |  |
| rest-custom-action | 1 | PASS | +guardrails +hidden-tests | 18 | 8 | 637,604 / 9,234 | $0.02 | 1m43s |  |
| rest-parent-child | 1 | FAIL | -guardrails +hidden-tests | 25 | 5 | 1,117,061 / 24,392 | $0.03 | 2m36s | PLAIN_SPRING_FALLBACK |
| rest-update-and-delete | 1 | PASS | +guardrails +hidden-tests | 26 | 7 | 1,002,779 / 14,006 | $0.03 | 2m03s |  |
| rest-validation | 1 | PASS | +guardrails +hidden-tests | 25 | 6 | 907,038 / 13,803 | $0.02 | 1m38s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 22 | 5 | 950,785 / 13,617 | $0.03 | 1m44s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 27 | 5 | 896,655 / 12,375 | $0.02 | 2m00s |  |
| security-role-based | 1 | PASS | +guardrails +hidden-tests | 19 | 5 | 719,553 / 13,252 | $0.02 | 1m40s |  |
| tenancy-isolation | 1 | PASS | +guardrails +hidden-tests | 15 | 4 | 614,498 / 11,732 | $0.02 | 1m26s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 13 | 3 | 357,659 / 8,316 | $0.01 | 57s |  |
| testing-integration-tests | 1 | FAIL | +guardrails +main-unchanged +agent-tests-written -agent-tests-pass +mutant-killed:pay-keeps-status +mutant-killed:double-payment-allowed +mutant-killed:amount-not-stored | 25 | 5 | 1,157,199 / 12,595 | $0.03 | 2m09s | TEST_OR_INFRASTRUCTURE |
| view-model | 1 | PASS | +guardrails +hidden-tests | 23 | 4 | 895,750 / 10,389 | $0.02 | 1m29s |  |

## Failures

### avro-avsc-contract#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 0/2 passed; com.example.shipping.ShipmentDispatchedAcceptanceTest.eventClassIsGeneratedFromTheSchema: Error creating bean with name 'com.example.shipping.ShipmentDispatchedAcceptanceTest': Injection of autowired dependencies failed | com.example.shipping.ShipmentDispatchedAcceptanceTest.dispatchPublishesTheContractEvent: Error creating bean with name 'com.example.shipping.ShipmentDis...`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/avro-avsc-contract-1`

### avro-publish-and-handle#1

**TEST_OR_INFRASTRUCTURE** (heuristic): the agent read the benchmark sources (hidden tests or reference)

- `hidden-tests: 0/1 passed; com.example.retail.SalePaidAvroAcceptanceTest.paidSalesEndUpInTheCashDrawer: Error creating bean with name 'com.example.retail.SalePaidAvroAcceptanceTest': Injection of autowired dependencies failed`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/avro-publish-and-handle-1`

### binary-upload-download#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 4/5 passed; com.example.documents.DocumentUploadAcceptanceTest.missingFilePartIsABadRequest: Status expected:<400> but was:<500>`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/binary-upload-download-1`

### events-multicast#1

**PLAIN_SPRING_FALLBACK** (heuristic): the agent wrote code Prefab is meant to generate

- `guardrails: generated-class-override in src/main/java/com/example/watch/application/PriceWatchRepository.java; spring-data-repository in src/main/java/com/example/watch/application/PriceWatchRepository.java; hand-written-consumer in src/main/java/com/example/watch/infrastructure/kafka/PriceWatchPriceChangedListener.java`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/watch/application/PriceWatchRepository.java`
- `guardrail spring-data-repository (FAIL) src/main/java/com/example/watch/application/PriceWatchRepository.java:10`
- `guardrail hand-written-consumer (FAIL) src/main/java/com/example/watch/infrastructure/kafka/PriceWatchPriceChangedListener.java:22`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/events-multicast-1`

### events-sealed-hierarchy#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 0/1 passed; com.example.banking.AccountEventsAcceptanceTest.depositsAndWithdrawalsArePublishedInOrder: Expected size: 2 but was: 0 in: []`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/events-sealed-hierarchy-1`

### list-full-text-search#1

**PLAIN_SPRING_FALLBACK** (heuristic): the agent wrote code Prefab is meant to generate

- `guardrails: hand-written-controller in src/main/java/com/example/blog/ArticleSearchController.java`
- `guardrail hand-written-controller (FAIL) src/main/java/com/example/blog/ArticleSearchController.java:22`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/list-full-text-search-1`

### mixin-custom-sql#1

**TEST_OR_INFRASTRUCTURE** (heuristic): the agent crashed or the environment failed: I added `totalRevenue(String status)` to the generated `com.example.ordering.application.OrderRepository`. `mvn verify` passes, including two new integration tests against a real Postgres container.  Prefab generates `OrderRepository` and doesn't let you edit it, so the method goes in a mixin: an interface annotated with `@RepositoryMixin(Order.class)`. The processor then adds it to the generated repository's supertypes. No second repository was created.  - **`src/main/java/com/example/ordering/application/OrderRepositoryMixin.java`**: declares `BigDecimal totalRevenue(String status)` with a native `@Query`:   `SELECT COALESCE(SUM("total"), 0) FROM "order" WHERE "status" = :status`   The sum runs in the database, and `COALESCE` returns `0` when no orders match. The table name is quoted because `order` is a reserved word in Postgres. - **`src/test/java/com/example/ordering/application/OrderRepositoryTest.java`**: an `@IntegrationTest(containers = true)` test that checks the zero case for an unused status, and that the sum over `SHIPPED` orders rises by the expected amount after creating and shipping two orders. It uses the existing `OrderClient` to create the orders. The test needs Docker because Prefab's in-memory test backend can't run native queries.  Docker's daemon wasn't running, only a stale socket was present. I started `dockerd` in the background to run the tests; the environment may need the same step before tests run.

- `hidden-tests: 0/2 passed; com.example.ordering.OrderRevenueAcceptanceTest.revenueIsZeroWithoutOrders: Failed to load ApplicationContext for [WebMergedContextConfiguration@98722ef testClass = com.example.ordering.OrderRevenueAcceptanceTest, locations = [], classes = [com.example.Application], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors = [], propertySourcePr...`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/mixin-custom-sql-1`

### mongodb-crud#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 0/2 passed; com.example.notes.NoteAcceptanceTest.notesCanBeCreatedArchivedAndDeleted: Failed to load ApplicationContext for [WebMergedContextConfiguration@56b751b1 testClass = com.example.notes.NoteAcceptanceTest, locations = [], classes = [com.example.Application], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors = [], propertySourceProperties = [...`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/mongodb-crud-1`

### rest-computed-field#1

**PLAIN_SPRING_FALLBACK** (heuristic): the agent wrote code Prefab is meant to generate

- `guardrails: generated-class-override in src/main/java/com/example/ordering/infrastructure/http/OrderController.java; hand-written-controller in src/main/java/com/example/ordering/infrastructure/http/OrderController.java`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/ordering/infrastructure/http/OrderController.java`
- `guardrail hand-written-controller (FAIL) src/main/java/com/example/ordering/infrastructure/http/OrderController.java:19`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/rest-computed-field-1`

### rest-parent-child#1

**PLAIN_SPRING_FALLBACK** (heuristic): the agent wrote code Prefab is meant to generate

- `guardrails: generated-class-override in src/main/java/com/example/planning/application/CreateTaskRequest.java; generated-class-override in src/main/java/com/example/planning/application/TaskRepository.java; spring-data-repository in src/main/java/com/example/planning/application/TaskRepository.java; generated-class-override in src/main/java/com/example/planning/application/TaskService.java; genera...`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/planning/application/CreateTaskRequest.java`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/planning/application/TaskRepository.java`
- `guardrail spring-data-repository (FAIL) src/main/java/com/example/planning/application/TaskRepository.java:9`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/planning/application/TaskService.java`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/planning/infrastructure/http/TaskController.java`
- `guardrail hand-written-controller (FAIL) src/main/java/com/example/planning/infrastructure/http/TaskController.java:19`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/rest-parent-child-1`

### testing-integration-tests#1

**TEST_OR_INFRASTRUCTURE** (heuristic): the agent crashed or the environment failed: I added an integration test class for the invoice API, and all 3 tests pass against a real PostgreSQL 18.6 container (`BUILD SUCCESS`).  The new file is `src/test/java/com/example/billing/InvoiceIntegrationTest.java`. Nothing under `src/main` changed, and the test directory is the only thing added to the repo.  The tests: - **`createdInvoiceCanBeReadBackWithAllItsFields`**: `POST /invoices`, then `GET` on the returned `Location`. It checks id, customer, amount (`199.90`), status `OPEN`, and that the response location is `/invoices/{id}`. - **`payingAnInvoiceMarksItAsPaid`**: pays an open invoice, expects `200`, and reads it back as `PAID`. - **`payingAnAlreadyPaidInvoiceIsRefusedWithConflict`**: pays twice, expects `409` on the second call, and confirms the status is still `PAID`.  Two things you should know: - **Prefab's default is in-memory, not Postgres.** The test class uses `@IntegrationTest(containers = true)` from `prefab-test` to force the Testcontainers Postgres setup. - **Version assertion.** The first draft asserted `version == 0` after creation, and that failed: Spring Data JDBC bumps `@Version` when the aggregate is first persisted, so a new invoice reads back as version `1`. The test now asserts `1` with a comment explaining why.  Environment note: the Docker daemon wasn't running when I started, and the Testcontainers run failed until I started `dockerd` in the background. The Docker socket file was present, but nothing was listening on it. I didn't change any Maven or Docker configuration.

- `agent-tests-pass: 3 tests, 3 failed (need at least 3); com.example.billing.InvoiceIntegrationTest.createdInvoiceCanBeReadBackWithAllItsFields: Failed to load ApplicationContext for [WebMergedContextConfiguration@7c682e26 testClass = com.example.billing.InvoiceIntegrationTest, locations = [], classes = [com.example.Application], contextInitializerClasses = [], activeProfiles = ["test"], propertySou...`

Logs: `/home/user/prefab/benchmark/results/20261124-111212-claude-haiku-5-5-none/attempts/testing-integration-tests-1`
