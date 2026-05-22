---
id: TASK-198
title: >-
  Address optimistic-lock retries in Pub/Sub UserEvent handlers for Channel and
  UserStatus
status: Done
assignee: []
created_date: '2026-05-13 18:33'
updated_date: '2026-05-21 06:21'
labels:
  - pubsub
  - reliability
  - follow-up
dependencies: []
references:
  - >-
    examples/pubsub/src/main/java/be/appify/prefab/example/pubsub/channel/Channel.java
  - >-
    examples/pubsub/src/main/java/be/appify/prefab/example/pubsub/user/UserStatus.java
  - >-
    examples/pubsub/src/main/java/be/appify/prefab/example/pubsub/user/UserEvent.java
priority: high
ordinal: 27200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
During TASK-135 investigation, repeated OptimisticLockingFailureException warnings were observed in Pub/Sub consumers for ChannelService.onUserSubscribed and UserStatusService.onMessageSent. Current partitioning keys and handler concurrency allow concurrent updates on the same aggregate, causing noisy retries and risk of retry exhaustion under load.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Define and implement a deterministic ordering/partitioning strategy so updates to the same Channel aggregate do not race across UserEvent.SubscribedToChannel deliveries.
- [x] #2 Define and implement a deterministic ordering/partitioning strategy so updates to the same UserStatus aggregate do not race across MessageSent deliveries.
- [x] #3 Pub/Sub integration tests run reliably for 20 consecutive runs without RetryException exhaustion in background consumer threads.
- [x] #4 Document the chosen ordering/concurrency trade-offs in backlog/docs/feature-guides.md (or relevant developer guide page).
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-05-14: Implemented deterministic hot-key stabilization for Pub/Sub handlers by setting Channel and UserStatus event-handler concurrency to 1.
2026-05-14: Expanded Pub/Sub ChannelSummary integration test to 20 scenario runs and verified no RetryException exhaustion in test logs.
2026-05-14: Added developer-guide documentation for event consumer ordering, deterministic projection IDs, and platform-specific trade-offs.
<!-- SECTION:NOTES:END -->
