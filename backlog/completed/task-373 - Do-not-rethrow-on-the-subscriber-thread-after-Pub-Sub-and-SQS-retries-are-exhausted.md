---
id: TASK-373
title: Do not rethrow on the subscriber thread after Pub/Sub and SQS retries are exhausted
status: Done
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
- [x] #1 A message whose retries are exhausted is nacked and logged without an exception on the subscriber thread
- [x] #2 The same holds for SQS, or the difference is documented
- [x] #3 The `Awaitility.doNotCatchUncaughtExceptionsByDefault()` workaround can be removed from the showcase `Shop`
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Decision: stop rethrowing; no configurable error handler. The nack (Pub/Sub) or leaving the message on the queue (SQS)
is the whole failure signal, and the error log carries the cause. A handler hook can be added later without changing
this default.

- `PubSubUtil.consume` and `SqsUtil.processMessage` no longer throw `RuntimeException(RetryException)` after logging.
- SQS: the throw was caught by the polling loop, which logged it a second time and skipped the remaining messages of
  the polled batch until their visibility timeout expired. Not rethrowing fixes that as well, so SQS has the same
  behaviour as Pub/Sub (AC #2).
- Tests: `GenericPubSubJsonTest.rawSubscriptionNacksWithoutThrowingWhenConsumerFails` (was `...NacksWhenConsumerFails`,
  asserted the throw) and the new `SqsUtilTest.messageStaysOnTheQueueWithoutThrowingWhenRetriesAreExhausted`.
- Removed `Awaitility.doNotCatchUncaughtExceptionsByDefault()` from the showcase `Shop`.
- Docs: note in `backlog/docs/configuration.md`. No annotation, attribute or generated artefact changed, so no cheat
  sheet, recipe or benchmark change (internal runtime behaviour only).
- The Pub/Sub emulator still does not dead-letter, so an event that can never succeed is redelivered for as long as
  the context runs; that is an emulator limitation and unchanged.
