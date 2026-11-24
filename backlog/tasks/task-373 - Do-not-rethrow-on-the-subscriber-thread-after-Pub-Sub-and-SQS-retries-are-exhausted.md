---
id: TASK-373
title: Do not rethrow on the subscriber thread after Pub/Sub and SQS retries are exhausted
status: To Do
assignee: []
created_date: '2026-11-24 07:40'
labels:
  - pubsub
  - sns-sqs
  - coverage-gap
dependencies: []
priority: medium
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`PubSubUtil.consume` (and `SqsUtil`) nack the message and log an error when the retries are exhausted, and then
throw `RuntimeException(RetryException)`. The listener runs on an executor (`request.executor().execute(..)`), so the
exception is never seen by the broker client; it ends in the uncaught-exception handler of the pool thread. The
message is already nacked, so the throw adds no information.

Found by the showcase on Pub/Sub (TASK-337.09): Kafka dead-letters a failing event quietly, but on Pub/Sub the
exception fails whichever Awaitility `await()` is running in an unrelated test, because Awaitility catches uncaught
exceptions by default. The showcase works around it with `Awaitility.doNotCatchUncaughtExceptionsByDefault()` in
`Shop`.

Decide whether to stop rethrowing after the nack, or to hand the failure to a configurable error handler. Tests that
assert the current behaviour (`GenericPubSubJsonTest.rawSubscriptionNacksWhenConsumerFails`) change with it.

Also note that the Pub/Sub emulator does not dead-letter, so an event that can never succeed (for example its
aggregate was removed) is redelivered for as long as the context runs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A message whose retries are exhausted is nacked and logged without an exception on the subscriber thread
- [ ] #2 The same holds for SQS, or the difference is documented
- [ ] #3 The `Awaitility.doNotCatchUncaughtExceptionsByDefault()` workaround can be removed from the showcase `Shop`
<!-- AC:END -->
