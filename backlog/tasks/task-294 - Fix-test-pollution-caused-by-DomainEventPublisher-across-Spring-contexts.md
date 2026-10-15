---
id: TASK-294
title: Fix test pollution caused by DomainEventPublisher across Spring contexts
status: Done
assignee: []
created_date: '2026-10-15 12:55'
labels:
  - testing
  - events
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When a test run uses multiple Spring application contexts (e.g. different `@SpringBootTest` configurations), `DomainEventPublisher` stays bound to a single context, so events published from aggregates in a later test may be delivered to the wrong context (or none). Introduce a Spring `TestExecutionListener` that activates the `DomainEventPublisher` implementation belonging to the context of the running test, and register it so it is enabled by default whenever Prefab is on the classpath (via `META-INF/spring.factories` `TestExecutionListener` entry).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A `TestExecutionListener` binds the `DomainEventPublisher` of the test's own context before each test (and restores state afterwards)
- [x] #2 The listener is registered by default through `spring.factories`, with no user configuration needed
- [x] #3 A regression test with two distinct contexts in one run proves events reach the correct context
- [x] #4 Existing example module tests run in one JVM without cross-context event leakage
<!-- AC:END -->

## Implementation Notes

Added `DomainEventPublisherTestExecutionListener` in the `test` module (binds the context's own publisher before each test, restores the previous one after), registered in `META-INF/spring.factories`. Regression test `DomainEventPublisherTestExecutionListenerTest` uses two distinct contexts. Since the listener is registered by default, the example modules pick it up automatically.
