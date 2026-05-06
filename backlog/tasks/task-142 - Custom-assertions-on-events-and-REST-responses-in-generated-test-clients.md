---
id: TASK-142
title: Custom assertions on events and REST responses in generated test clients
status: Done
assignee: []
created_date: '2026-04-27 13:18'
updated_date: '2026-05-06 06:05'
labels:
  - test
  - annotation-processor
  - rest
dependencies: []
priority: medium
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Integration tests currently assert on raw return values (IDs, response objects) or use Consumer<ListAssert<V>> for events. There is no consistent, fluent assertion API generated alongside the test clients. Each generated test client method (create, update, delete, get, getList) should return a typed assertion object that wraps the ResultActions / response, allowing callers to express custom assertions fluently. Similarly, the event consumer assertion step should support typed, domain-specific assertion helpers so tests read as clear specifications.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Each generated test client operation method (create, update, delete, get, getList) returns a typed response assertion object instead of a raw value
- [x] #2 The response assertion object exposes the existing convenience accessors (e.g. id(), response()) and a fluent andAssert(Consumer<ResponseAssert>) method for custom assertions
- [x] #3 The response assertion object wraps the underlying ResultActions so that MockMvc status and header assertions remain accessible
- [x] #4 EventConsumerWhereStep gains an overload where(Class<A> assertClass, Consumer<A> assertion) that instantiates a custom AssertJ assertion class over the received event list
- [x] #5 Existing callers of generated test client methods and EventConsumerWhereStep.where(Consumer<ListAssert<V>>) continue to compile without changes (backwards compatible)
- [x] #6 Unit tests cover: custom REST response assertion, custom event assertion, and backwards-compatible default where() call
<!-- AC:END -->
