---
id: task-020
title: Sealed interfaces for events
status: Done
assignee: []
created_date: '2025-10-10 13:37'
updated_date: '2025-12-23 13:44'
labels: []
dependencies: []
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow multiple types of events on the same topic using a sealed interface as parent. Fail compilation when multiple events are publishing to the same topic that do not share a sealed interface.

Subtasks:
- [x] Adopt Kafka consumer writer
- [x] Adopt Kafka producer writer
- [x] Fail Kafka compilation when missing supertype
- [x] Adopt PubSub consumer writer
- [x] Adopt PubSub producer writer
- [x] Fail PubSub compilation when missing supertype
- [x] Add Kafka example
- [x] Add PubSub example
<!-- SECTION:DESCRIPTION:END -->
