---
id: TASK-302.01
title: 'Stream tracing foundations: config, ingress, publish and test API'
status: Done
assignee: []
created_date: '2026-10-30 13:40'
labels:
  - streams
  - tracing
dependencies: []
parent_task_id: TASK-302
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Slice 1 of TASK-302 (see its plan). Add `TracingConfig` (`prefab.streams.tracing.enabled`, `prefab.streams.tracing.max-correlation-ids`), `MessageTracing` header constants, `StreamRecord` accessors, an ingress stage on `from` (moves the consumed message's ID, or a random one, into the in-flight origin set) and a publish stage before `to` (mints the message ID). Header propagation through 1:1 operators (`filter`, `branch`, `merge`, `repartition`, `deduplicate`, `map`, `flatMap`, `keyBy`). Test API: send with message ID, read messages with IDs, enable tracing in the topology test extension.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Tracing is disabled by default and leaves the topology untouched.
- [x] #2 With tracing enabled every message published by `to` has a fresh message ID.
- [x] #3 Consumed messages without an ID get a random one; existing IDs are used as the correlation of derived messages.
- [x] #4 Chains of in-memory operators keep the correlation of the consumed message.
- [x] #5 Messages not originating from another message have no correlation header.
- [x] #6 Topology tests can send messages with an ID and assert message and correlation IDs.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added TracingConfig (factories disabled()/withTracing()), MessageTracing, KafkaMessageTracing ingress/publish stages, StreamRecord.messageId()/correlationIds(), and test API support (sendWithMessageId, messages(), TopologyTestExtension.withTracing()). Tests: MessageTracingTopologyTest, MessageTracingDisabledTopologyTest. Full streams suite passes. Records without an origin message are only produced by punctuators (slice 2).
<!-- SECTION:NOTES:END -->
