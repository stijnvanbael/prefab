---
id: TASK-290
title: Use per-topic Pub/Sub subscription names for multi-topic events
status: Done
assignee: []
created_date: '2026-09-29 11:30'
labels:
  - events
  - pubsub
  - bug
dependencies:
  - TASK-289
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while fixing TASK-289. `PubSubSubscriberWriter` gives every subscription of a handler the same
name, `{owner}-on-{event}` (e.g. `user-service-on-user-event`), regardless of the topic.
`PubSubUtil.ensureSubscriptionExists` only creates a subscription when that name does not exist yet,
and a Pub/Sub subscription is bound to exactly one topic.

So when an event has multiple topics (several `@Event` topics, or since TASK-289 a `${...}`/`#{...}`
expression that expands to several topics), only the first topic gets a subscription. The handler
silently never receives messages from the other topics.

SQS is not affected: a single queue can be subscribed to several SNS topics.

**Decision needed:** making the name unique per topic (e.g. appending the topic name) renames existing
subscriptions. Deployed single-topic consumers would get a new subscription and lose unacknowledged
messages on the old one. Options:
- Keep the current name when the handler resolves to a single topic; only suffix when there are several.
- Always suffix and document the migration.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every resolved topic of a multi-topic event gets its own Pub/Sub subscription
- [x] #2 Subscription names of existing single-topic handlers are handled per the chosen migration strategy
- [x] #3 Tests cover multiple static topics and an expression expanding to multiple topics
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Decision: always include the topic in the subscription name (option 2), for consistent naming regardless of
  how many topics an expression resolves to. Migration documented in `backlog/docs/configuration.md`
  ("Subscription names").
- `PubSubSubscriberWriter.subscriptionName` generates `{owner}-on-{event}-{topic}`: a string literal for
  literal topics, `"{owner}-on-{event}-" + topic` inside the loop for `${...}`/`#{...}` topics.
  Pub/Sub topic and subscription names share the same character set, so the result is always valid
  (the 255-character limit still applies).
- `PubSubUtil` and `SubscriptionRequest` are unchanged; the public `subscribe(topic, subscription, ...)` API
  still uses the given name as-is.
- Tests: `PubSubSpelTopicTest` verifies at runtime that each resolved topic gets its own subscription name;
  `PubSubSubscriberWriterTest` assertions and expected fixtures updated.
- Not changed: the Terraform GCP output names subscriptions `{topic}-subscription`, which has never matched
  the runtime names.
<!-- SECTION:NOTES:END -->
