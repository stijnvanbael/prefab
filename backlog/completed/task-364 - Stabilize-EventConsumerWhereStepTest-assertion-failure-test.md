---
id: TASK-364
title: Stabilize EventConsumerWhereStepTest assertion failure test
status: Done
assignee: []
created_date: '2026-11-23T11:40:00Z'
updated_date: '2026-11-24T13:56:39Z'
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
- [x] #1 The test reliably verifies propagation of a failed custom assertion without depending on timing races.
- [x] #2 Repeated test and reactor runs pass without changing the intended assertion behavior.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Root cause: `EventConsumerAssert.where` uses Awaitility `untilAsserted`, so a deliberately failing assertion is retried and only surfaced after its timeout. Under reactor load the test can hit the deadline before the callback runs, producing a timeout rather than the assertion failure it intended to verify. The custom-assert adapter is the `EventConsumerWhereStep` default overload; its failure propagation is now tested using a synchronous `where` implementation, with no polling or wall-clock deadline. Production polling/retry behavior is unchanged.

Verification: the focused test class passed five consecutive runs; `mvn -q -pl test -am test` passed twice. A full `mvn -q test` run reached `showcase-pubsub-example` and failed two separate `CustomerOrderHistoryIntegrationTest` event-delivery assertions (20-second Awaitility timeouts); it did not fail in the changed test/module. The full reactor is not green for this unrelated integration-test issue.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Removed Awaitility timing from the custom assertion failure-propagation test by exercising the `EventConsumerWhereStep` default overload synchronously. The intended `AssertionError` is now asserted directly, and production retry behavior is unchanged. The focused class passed five consecutive runs and the test-module reactor passed twice; the full reactor remains blocked by unrelated showcase Pub/Sub event-delivery timeouts.
<!-- SECTION:FINAL_SUMMARY:END -->
