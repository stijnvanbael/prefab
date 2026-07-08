---
id: TASK-261
title: Ignore unknown event types on subscribed topics
status: To Do
assignee: []
created_date: '2026-07-08 08:37'
updated_date: '2026-07-08 08:38'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When external producers publish new event types on a topic that this application already consumes, generated consumers must not fail processing. Unknown event types should be safely ignored so known event processing continues across Kafka, SNS/SQS, and Pub/Sub.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Kafka consumers continue running when a message with an unknown event type arrives on a subscribed topic.
- [ ] #2 SNS/SQS consumers continue running when a message with an unknown event type arrives on a subscribed topic.
- [ ] #3 Pub/Sub consumers continue running when a message with an unknown event type arrives on a subscribed topic.
- [ ] #4 Unknown event messages are ignored and do not trigger user-defined handler methods for known event types.
- [ ] #5 Automated tests cover unknown-event handling for Kafka, SNS/SQS, and Pub/Sub consumer paths and verify processing continues for subsequent known events.
<!-- AC:END -->
