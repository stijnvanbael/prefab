---
id: task-175
title: Add in-memory persistence and messaging backend for tests
status: To Do
assignee: []
created_date: '2026-05-08 16:37'
labels: []
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a `prefab-test-inmemory` module that replaces PostgreSQL/MongoDB with an in-memory repository and replaces Kafka/Pub/Sub/SNS with a synchronous in-process event bus. Allows integration tests that don't need real infrastructure to run without containers, greatly reducing feedback loop time.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 prefab-test-inmemory module ships a ConcurrentHashMap-backed repository implementation
- [ ] #2 An in-process synchronous event bus replaces Kafka/Pub/Sub/SNS in tests using this module
- [ ] #3 Existing integration tests can opt-in by swapping the persistence/messaging module dependency
- [ ] #4 The in-memory backend is documented in the Developer Guide
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
