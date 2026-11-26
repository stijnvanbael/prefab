---
id: TASK-400
title: Track lineage from incoming requests and events to outgoing ones
status: To Do
assignee: []
created_date: '2026-11-26 14:30'
labels:
  - core
  - events
  - observability
dependencies: []
references:
  - backlog/completed/task-302 - Opt-in-message-and-correlation-IDs-for-Prefab-streams.md
  - backlog/tasks/task-366 - Opt-in-Micrometer-observations-for-generated-operations-and-event-consumers.md
  - backlog/tasks/task-110 - Outgoing-webhook-delivery-Webhook.md
priority: medium
type: feature
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
As a developer operating a Prefab application, I want every outgoing event or request to be traceable to the
incoming request or event that caused it, so that I can follow a business flow through the application (and across
applications) and answer "why was this event published?" or "what did this request lead to?".

TASK-302 introduced message IDs and correlation IDs for Prefab **streams**. This task brings the same lineage model
to the rest of the application: generated REST endpoints, event handlers, published domain events and outgoing
requests.

### Concepts (aligned with TASK-302)
- **Message ID**: unique identifier of one incoming request, one consumed event or one published event, carried as
  metadata (HTTP header, broker message header), never in the payload.
- **Correlation IDs**: the message IDs of the incoming request(s) or event(s) that caused a message.

### Rules
1. An incoming HTTP request to a generated endpoint gets a message ID; an ID supplied by the caller is preserved.
2. A consumed event keeps its message ID; an event without one gets a random ID that is used within the application
   (not written back to the source).
3. Every event published while handling a request or event carries correlation IDs referring to that request or
   event, whether it is published from a `@Create`/`@Update` method, an `@EventHandler`, a `@Multicast` fan-out or
   user code using `EventPublisher`.
4. Outgoing requests made while handling a request or event (webhooks, TASK-110, and HTTP clients the application
   uses) carry the same lineage headers.
5. A message that does not originate from a request or event (scheduled job, startup data) has no correlation IDs;
   the metadata is absent rather than empty.
6. Lineage propagates across asynchronous boundaries the application controls (async commit, outbox, retries,
   dead-letter handling), so a retried or replayed event still points at its origin.
7. Lineage is available to user code (current message ID and correlation IDs) and to tests, and appears in logs
   through the MDC.

### Design questions to settle during analysis
- Opt-in or default; how it combines with TASK-302's stream switch so both use one header format.
- Relation to W3C Trace Context / OpenTelemetry (`traceparent`) and Micrometer tracing (TASK-366): reuse or bridge
  rather than invent a parallel mechanism where possible.
- Header names and representation per broker (Kafka, Pub/Sub, SNS/SQS) and for MongoDB/Postgres outbox rows.
- Carrying context across threads and virtual threads (`ScopedValue` rather than `ThreadLocal`).
- Whether lineage is persisted with aggregates (for example last-modifying message ID) or only travels with messages.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An event published while handling a generated REST request carries a correlation ID equal to that request's message ID
- [ ] #2 An event published by an event handler, including `@Multicast` fan-out, carries the consumed event's message ID as correlation ID, on every supported broker
- [ ] #3 Outgoing requests made while handling a request or event carry the lineage headers
- [ ] #4 Caller-supplied message IDs and IDs on consumed events are preserved, never overwritten
- [ ] #5 Messages without a cause carry no correlation IDs
- [ ] #6 Lineage survives retries, async commit and dead-letter handling
- [ ] #7 Current lineage is readable from user code and tests, and is in the logging MDC
- [ ] #8 The header format is shared with stream tracing (TASK-302) and documented, with the decision on W3C Trace Context recorded as an ADR
<!-- AC:END -->
