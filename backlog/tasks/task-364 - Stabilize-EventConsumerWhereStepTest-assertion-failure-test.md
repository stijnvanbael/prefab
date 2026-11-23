---
id: TASK-364
title: Stabilize EventConsumerWhereStepTest assertion failure test
status: To Do
assignee: []
created_date: '2026-11-23 11:40'
labels:
  - testing
  - bug
dependencies: []
priority: medium
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`EventConsumerWhereStepTest.customAssertClassPropagatesAssertionFailures` intermittently fails because it expects
an `AssertionError` as the cause but receives a `TimeoutException` instead. A showcase reactor test run exposed
the failure; the same reactor passed on retry. TASK-303 recorded an earlier occurrence. Investigate the timeout and
make the test reliably verify propagation of the assertion failure.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The test reliably verifies propagation of a failed custom assertion without depending on timing races.
- [ ] #2 Repeated test and reactor runs pass without changing the intended assertion behavior.
<!-- AC:END -->
