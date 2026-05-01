---
id: TASK-155
title: Outbox relay - add retry limits and dead-letter handling for failed entries
status: To Do
assignee: []
created_date: '2026-05-01 00:00'
updated_date: '2026-05-01 00:00'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The current `OutboxRelayService` retries every failed outbox entry indefinitely on each poll cycle.
If an entry fails (e.g. event class not found, deserialisation error), the warning is logged on every cycle
without any backoff or dead-letter mechanism. This can flood logs and waste resources.

Additionally, `Class.forName()` is called with the `eventType` value stored in the outbox database.
Although this value is written by the framework itself, a compromised database could inject arbitrary class
names. A registry or allow-list of known event types would reduce the attack surface.

Affected file: `core/src/main/java/be/appify/prefab/core/outbox/OutboxRelayService.java`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add `retryCount` and `lastAttemptAt` fields to `OutboxEntry` and the outbox schema
- [ ] #2 After N configurable failed attempts, move the entry to a dead-letter state or table
- [ ] #3 Implement exponential back-off between retries using `lastAttemptAt`
- [ ] #4 Replace unconstrained `Class.forName()` with a registry or allow-list of known event types
- [ ] #5 Add configuration properties: `prefab.outbox.max-retries` (default 5) and `prefab.outbox.backoff-multiplier`
<!-- AC:END -->
