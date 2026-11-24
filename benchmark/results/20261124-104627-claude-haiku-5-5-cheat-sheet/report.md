# Prefab benchmark: claude-haiku-5-5 with context `cheat-sheet`

| | |
|---|---|
| Run | 20261124-104627-claude-haiku-5-5-cheat-sheet |
| Agent | claude-code/claude-haiku-5-5 |
| Context | cheat-sheet |
| Prefab | 0.13.3-SNAPSHOT @ e2824ba |
| Started | 2026-11-24T10:46:27.051419190Z |
| Attempts | 32 (32 tasks x 1) |
| **Pass rate** | **24/32 = 75% (95% CI 58-87%)** |
| Tasks passed in every attempt | 24 |
| Cost | $0.60 |
| Tokens (input incl. cache / output) | 21,954,189 / 332,772 |
| Median turns / builds / agent time | 16 / 4 / 1m19s |

## Failure categories

| Category | Attempts |
|---|---|
| DOC_GAP | 5 |
| MISLEADING_DIAGNOSTIC | 0 |
| API_MISUSE | 0 |
| PLAIN_SPRING_FALLBACK | 1 |
| TEST_OR_INFRASTRUCTURE | 2 |

## By task category

| Category | Passed |
|---|---|
| audit | 1/1 |
| avro | 0/2 |
| binary | 0/1 |
| diagnostics | 1/1 |
| events-handling | 2/3 |
| events-publishing | 2/2 |
| lists | 4/5 |
| mongodb | 0/1 |
| multi-tenancy | 1/1 |
| persistence | 1/1 |
| repository-mixins | 1/2 |
| rest-crud | 7/7 |
| security | 2/2 |
| testing | 1/2 |
| views | 1/1 |

## Attempts

| Task | # | Result | Checks | Turns | Builds | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|
| audit-trail | 1 | PASS | +guardrails +hidden-tests | 27 | 4 | 931,754 / 15,531 | $0.02 | 1m57s |  |
| avro-avsc-contract | 1 | FAIL | +guardrails -hidden-tests | 24 | 6 | 1,139,167 / 17,158 | $0.03 | 2m46s | DOC_GAP |
| avro-publish-and-handle | 1 | FAIL | +guardrails -hidden-tests | 28 | 3 | 1,308,664 / 15,134 | $0.03 | 2m35s | DOC_GAP |
| binary-upload-download | 1 | FAIL | +guardrails -hidden-tests | 25 | 6 | 1,070,389 / 18,311 | $0.03 | 2m41s | DOC_GAP |
| diagnostic-fix-build | 1 | PASS | +guardrails +hidden-tests | 13 | 4 | 372,937 / 6,330 | $0.01 | 1m09s |  |
| events-create-from-event | 1 | PASS | +guardrails +hidden-tests | 13 | 4 | 421,956 / 6,909 | $0.01 | 1m16s |  |
| events-multicast | 1 | FAIL | -guardrails +hidden-tests | 13 | 5 | 391,497 / 6,761 | $0.01 | 1m18s | PLAIN_SPRING_FALLBACK |
| events-publish-on-create | 1 | PASS | +guardrails +hidden-tests | 21 | 2 | 802,850 / 9,659 | $0.02 | 1m11s |  |
| events-sealed-hierarchy | 1 | PASS | +guardrails +hidden-tests | 34 | 6 | 1,604,635 / 21,456 | $0.04 | 3m01s |  |
| events-update-by-reference | 1 | PASS | +guardrails +hidden-tests | 13 | 3 | 445,105 / 7,755 | $0.01 | 58s |  |
| list-autocomplete | 1 | PASS | +guardrails +hidden-tests | 16 | 2 | 451,418 / 6,316 | $0.01 | 50s |  |
| list-filters | 1 | PASS | +guardrails +hidden-tests | 14 | 4 | 472,109 / 8,545 | $0.01 | 1m20s |  |
| list-full-text-search | 1 | FAIL | +guardrails -hidden-tests | 28 | 8 | 1,392,475 / 15,043 | $0.03 | 8m13s | DOC_GAP |
| list-paged-sorted | 1 | PASS | +guardrails +hidden-tests | 25 | 6 | 838,849 / 16,830 | $0.02 | 2m05s |  |
| list-range-filter | 1 | PASS | +guardrails +hidden-tests | 14 | 4 | 469,996 / 6,448 | $0.01 | 53s |  |
| mixin-custom-sql | 1 | FAIL | +guardrails -hidden-tests | 24 | 7 | 985,023 / 10,981 | $0.02 | 10m04s | TEST_OR_INFRASTRUCTURE |
| mixin-derived-query | 1 | PASS | +guardrails +hidden-tests | 14 | 3 | 367,989 / 6,484 | $0.01 | 46s |  |
| mongodb-crud | 1 | FAIL | +guardrails -hidden-tests | 27 | 8 | 1,434,110 / 15,366 | $0.03 | 3m27s | DOC_GAP |
| persistence-evolve-schema | 1 | PASS | +guardrails +hidden-tests | 19 | 4 | 675,086 / 9,980 | $0.02 | 1m16s |  |
| rest-computed-field | 1 | PASS | +guardrails +hidden-tests | 12 | 4 | 372,153 / 6,075 | $0.01 | 56s |  |
| rest-create-and-get | 1 | PASS | +guardrails +hidden-tests | 10 | 3 | 315,501 / 5,786 | $0.01 | 56s |  |
| rest-custom-action | 1 | PASS | +guardrails +hidden-tests | 12 | 5 | 379,864 / 6,768 | $0.01 | 1m13s |  |
| rest-parent-child | 1 | PASS | +guardrails +hidden-tests | 18 | 7 | 629,225 / 10,890 | $0.02 | 1m54s |  |
| rest-update-and-delete | 1 | PASS | +guardrails +hidden-tests | 14 | 7 | 434,743 / 6,462 | $0.01 | 1m19s |  |
| rest-validation | 1 | PASS | +guardrails +hidden-tests | 16 | 5 | 436,310 / 7,645 | $0.01 | 1m15s |  |
| rest-value-object | 1 | PASS | +guardrails +hidden-tests | 13 | 4 | 435,192 / 7,457 | $0.01 | 1m08s |  |
| security-public-endpoints | 1 | PASS | +guardrails +hidden-tests | 16 | 4 | 480,126 / 9,291 | $0.01 | 1m42s |  |
| security-role-based | 1 | PASS | +guardrails +hidden-tests | 16 | 4 | 557,983 / 11,041 | $0.02 | 1m25s |  |
| tenancy-isolation | 1 | PASS | +guardrails +hidden-tests | 16 | 6 | 572,131 / 13,557 | $0.02 | 2m08s |  |
| testing-domain-events-unit | 1 | PASS | +guardrails +main-unchanged +agent-tests-written +unit-tests-only +agent-tests-pass +mutant-killed:withdraw-publishes-deposit +mutant-killed:deposit-not-published +mutant-killed:overdraft-allowed | 10 | 2 | 251,563 / 6,288 | $0.01 | 44s |  |
| testing-integration-tests | 1 | FAIL | +guardrails +main-unchanged +agent-tests-written -agent-tests-pass +mutant-killed:pay-keeps-status +mutant-killed:double-payment-allowed +mutant-killed:amount-not-stored | 17 | 3 | 594,809 / 10,263 | $0.02 | 1m52s | TEST_OR_INFRASTRUCTURE |
| view-model | 1 | PASS | +guardrails +hidden-tests | 21 | 4 | 918,580 / 10,252 | $0.02 | 1m31s |  |

## Failures

### avro-avsc-contract#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 0/2 passed; com.example.shipping.ShipmentDispatchedAcceptanceTest.eventClassIsGeneratedFromTheSchema: Error creating bean with name 'com.example.shipping.ShipmentDispatchedAcceptanceTest': Injection of autowired dependencies failed | com.example.shipping.ShipmentDispatchedAcceptanceTest.dispatchPublishesTheContractEvent: Error creating bean with name 'com.example.shipping.ShipmentDis...`

Logs: `/home/user/prefab/benchmark/results/20261124-104627-claude-haiku-5-5-cheat-sheet/attempts/avro-avsc-contract-1`

### avro-publish-and-handle#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 0/1 passed; com.example.retail.SalePaidAvroAcceptanceTest.paidSalesEndUpInTheCashDrawer: Error creating bean with name 'com.example.retail.SalePaidAvroAcceptanceTest': Injection of autowired dependencies failed`

Logs: `/home/user/prefab/benchmark/results/20261124-104627-claude-haiku-5-5-cheat-sheet/attempts/avro-publish-and-handle-1`

### binary-upload-download#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 4/5 passed; com.example.documents.DocumentUploadAcceptanceTest.missingFilePartIsABadRequest: Status expected:<400> but was:<500>`

Logs: `/home/user/prefab/benchmark/results/20261124-104627-claude-haiku-5-5-cheat-sheet/attempts/binary-upload-download-1`

### events-multicast#1

**PLAIN_SPRING_FALLBACK** (heuristic): the agent wrote code Prefab is meant to generate

- `guardrails: generated-class-override in src/main/java/com/example/watch/PriceWatchRepository.java`
- `guardrail generated-class-override (FAIL) src/main/java/com/example/watch/PriceWatchRepository.java`

Logs: `/home/user/prefab/benchmark/results/20261124-104627-claude-haiku-5-5-cheat-sheet/attempts/events-multicast-1`

### list-full-text-search#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 2/3 passed; com.example.blog.ArticleSearchAcceptanceTest.matchesIgnoringCaseInBothFields: Expecting actual:   [] to contain exactly in any order:   ["Spring tips", "Gardening in spring"] but could not find the following elements:   ["Spring tips", "Gardening in spring"]`

Logs: `/home/user/prefab/benchmark/results/20261124-104627-claude-haiku-5-5-cheat-sheet/attempts/list-full-text-search-1`

### mixin-custom-sql#1

**TEST_OR_INFRASTRUCTURE** (heuristic): the agent crashed or the environment failed: `OrderRepository` now has `BigDecimal totalRevenue(String status)`, and `mvn test` passes (2 new tests, both green against a real Postgres container).  `OrderRepository` is generated by Prefab, so I didn't write it by hand. I added a `@RepositoryMixin(Order.class)` interface, `OrderRepositoryMixin`, in `com.example.ordering.application`. Prefab makes the generated `OrderRepository` extend it, so the method is on `com.example.ordering.application.OrderRepository` and there is no second repository. The method is a single `@Query`:  ```sql SELECT COALESCE(SUM(o."total"), 0) FROM "order" o WHERE o."status" = :status ```  The table name is quoted because `order` is a reserved word. `COALESCE` gives `0` when no rows match.  The new tests are in `src/test/java/com/example/ordering/application/OrderRepositoryTest.java`. They cover the empty case and a case that checks sums only include the requested status. They use `@IntegrationTest(containers = true)` because the in-memory test backend refuses native queries.  The Docker daemon was not running. `service docker start` failed, so I started `dockerd` directly in the background. The tests depend on that daemon staying up, and the sandbox had already restarted it once during the run. Nothing is committed; the changes are in the working tree.

- `hidden-tests: 0/2 passed; com.example.ordering.OrderRevenueAcceptanceTest.revenueIsZeroWithoutOrders: Failed to load ApplicationContext for [WebMergedContextConfiguration@22429a11 testClass = com.example.ordering.OrderRevenueAcceptanceTest, locations = [], classes = [com.example.Application], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors = [], propertySourceP...`

Logs: `/home/user/prefab/benchmark/results/20261124-104627-claude-haiku-5-5-cheat-sheet/attempts/mixin-custom-sql-1`

### mongodb-crud#1

**DOC_GAP** (heuristic): the project builds but behaves differently than asked; review the diff to confirm

- `hidden-tests: 0/2 passed; com.example.notes.NoteAcceptanceTest.notesCanBeCreatedArchivedAndDeleted: Failed to load ApplicationContext for [WebMergedContextConfiguration@d978ab9 testClass = com.example.notes.NoteAcceptanceTest, locations = [], classes = [com.example.Application], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors = [], propertySourceProperties = ["...`

Logs: `/home/user/prefab/benchmark/results/20261124-104627-claude-haiku-5-5-cheat-sheet/attempts/mongodb-crud-1`

### testing-integration-tests#1

**TEST_OR_INFRASTRUCTURE** (heuristic): the agent crashed or the environment failed: I added `src/test/java/com/example/billing/InvoiceIntegrationTest.java`, and `mvn test` passes: 3 tests, 0 failures. The tests run against a real PostgreSQL container (`jdbc:postgresql://localhost:…`), which the `@IntegrationTest(containers = true)` annotation starts. No production code changed.  The three tests: - **Create and read back:** `POST /invoices` followed by `GET /invoices/{id}` checks id, version, customer, amount, and status (`OPEN`). - **Pay:** after `POST /invoices/{id}/pay`, the invoice reads back as `PAID`. - **Pay twice:** a second pay returns `409 Conflict`, and the invoice stays `PAID`.  The tests use the generated `InvoiceClient` and `InvoiceResponseAssert`, plus `MockMvc` for the 409 case, since the client's `pay()` asserts 200.  Two details that differ from what you might assume: - Version is `1` after create, not `0`. Spring Data JDBC sets it on insert, so the test asserts `1`. - The amount column comes back with scale 4 (`42.0000`), so the tests compare amounts with `isEqualByComparingTo` rather than the generated `hasAmount`, which uses `equals`.  Docker's daemon was not running at the start: the socket was stale and `dockerd` had exited. I started it with `dockerd` (log in `/tmp/dockerd.log`). That changed the host's Docker state outside the project, so it's worth knowing if you run this elsewhere.

- `agent-tests-pass: 3 tests, 3 failed (need at least 3); com.example.billing.InvoiceIntegrationTest.payingAnInvoiceMarksItAsPaid: Failed to load ApplicationContext for [WebMergedContextConfiguration@54f373d4 testClass = com.example.billing.InvoiceIntegrationTest, locations = [], classes = [com.example.Application], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors ...`

Logs: `/home/user/prefab/benchmark/results/20261124-104627-claude-haiku-5-5-cheat-sheet/attempts/testing-integration-tests-1`
