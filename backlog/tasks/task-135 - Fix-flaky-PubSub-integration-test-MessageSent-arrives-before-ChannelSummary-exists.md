---
id: TASK-135
title: >-
  Fix flaky PubSub integration test: MessageSent arrives before ChannelSummary
  exists
status: To Do
assignee: []
created_date: '2026-04-21 17:08'
updated_date: '2026-04-21 17:08'
labels:
  - pubsub
  - reliability
  - bug
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The ChannelSummaryIntegrationTest (and related tests) are flaky under PubSub because a MessageSent event is consumed before the ChannelSummary aggregate has been created by the ChannelCreated event handler. This manifests as an IllegalStateException: 'No aggregates found for event: MessageSent[...]'. Multiple root causes have been identified that must all be addressed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 PubSub messages for ChannelCreated and MessageSent on the same logical entity are published with an ordering key so Pub/Sub delivery order is guaranteed
- [ ] #2 The Multicast event handler uses a create-or-update pattern: when no aggregate is found it creates one instead of throwing an exception, preventing failures caused by processing order
- [ ] #3 When retry is exhausted the message is nacked to PubSub so it is redelivered after a meaningful backoff, rather than only retrying in-process while holding the ack deadline
- [ ] #4 ChannelCreated and MessageSent events that share a channel are published to a single ordered topic (or with the channel ID as ordering key) so that different-subscription delivery cannot interleave them
- [ ] #5 The integration test verifies that sending a message after creating a channel reliably updates ChannelSummary without flakiness across at least 10 consecutive runs
- [ ] #6 Transaction commit visibility is handled: the consumer waits (via retry with meaningful backoff) until the ChannelSummary written by ChannelCreated is visible in the read replica / same DB session before processing MessageSent
<!-- AC:END -->
