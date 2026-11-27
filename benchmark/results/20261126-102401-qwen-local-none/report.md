# Prefab benchmark: qwen3-coder:30b with context `none`

| | |
|---|---|
| Run | 20261126-102401-qwen-local-none |
| Agent | claude-code/qwen3-coder:30b |
| Context | none |
| Prefab | 0.13.3-SNAPSHOT @ a9b1d9bc-dirty |
| Started | 2026-11-26T10:24:01.390740Z |
| Attempts | 1 (1 tasks x 1) |
| **Pass rate** | **0/1 = 0% (95% CI 0-79%)** |
| Tasks passed in every attempt | 0 |
| Cost | $2,06 |
| Tokens (input incl. cache / output) | 1.037.415 / 10.390 |
| Median turns / builds / agent time | 46 / 9 / 15m03s |
| Median time to first green build | 51s (1/1 attempts) |
| Generated classes overridden or hand-written | 3 (in 1 attempts) |

## By tier

| Tier | Passed | 95% CI | Median turns | Median time to first green build | Overrides |
|---|---|---|---|---|---|
| quick | 0/1 | 0% (95% CI 0-79%) | 46 | 51s (1/1 attempts) | 3 (in 1 attempts) |

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
| rest-crud | 0/1 |

## Attempts

| Task | Tier | # | Result | Checks | Turns | Builds | First green | Overrides | Tokens in/out | Cost | Agent time | Failure |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| rest-create-and-get | quick | 1 | FAIL | -guardrails -hidden-tests | 46 | 9 | 51s | 3 | 1.037.415 / 10.390 | $2,06 | 15m03s | PLAIN_SPRING_FALLBACK |

## Failures

### rest-create-and-get#1

**PLAIN_SPRING_FALLBACK** (manual): Without docs, hand-wrote a JPA entity, controller and DTO, removed validation instead of adding the starter, added H2 for its own tests and reported success after stopping them. 

- `guardrails: jpa in src/main/java/com/example/catalog/Book.java; hand-written-controller in src/main/java/com/example/catalog/BookController.java; spring-data-repository in src/main/java/com/example/catalog/BookRepository.java`
- `hidden-tests: 0/3 passed; com.example.catalog.BookAcceptanceTest.titleIsRequired: Failed to load ApplicationContext for [WebMergedContextConfiguration@31b741e2 testClass = com.example.catalog.BookAcceptanceTest, locations = [], classes = [com.example.Application], contextInitializerClasses = [], activeProfiles = ["test"], propertySourceDescriptors = [], propertySourceProperties = ["org.springframe...`
- `guardrail jpa (FAIL) src/main/java/com/example/catalog/Book.java:3`
- `guardrail hand-written-controller (FAIL) src/main/java/com/example/catalog/BookController.java:11`
- `guardrail spring-data-repository (FAIL) src/main/java/com/example/catalog/BookRepository.java:7`

Logs: `/Users/stijnvanbael/IdeaProjects/appify/prefab/benchmark/results/20261126-102401-qwen-local-none/attempts/rest-create-and-get-1`
