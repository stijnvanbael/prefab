---
id: TASK-365
title: '@Multicast event handler fails when the query finds no aggregates'
status: To Do
assignee: []
created_date: '2026-11-23 07:10'
labels:
  - events
dependencies: []
priority: medium
type: bug
ordinal: 199000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A `@Multicast` handler without a static companion method throws `IllegalStateException("No aggregates found for
event: ...")` when its query method returns no aggregates (`MulticastEventHandlerWriter`). The consumer retries and
then dead-letters the message.

That is the right outcome when the target should exist but is created by another event that has not been processed
yet. It is wrong when zero targets is a normal situation, e.g. "when a customer is deactivated, cancel all their
orders": a customer without orders dead-letters `CustomerDeactivated`.

Found while writing the ordering slice of the showcase (TASK-337.04). The showcase avoids it: its multicasts only
target aggregates that always exist (stock levels, order histories), and the deactivation test only deactivates
customers that have orders.

Options: an opt-in attribute on `@Multicast` (e.g. `failWhenEmpty = false`) that makes an empty result a no-op,
documented in the developer guide next to the static-companion behaviour.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A multicast handler can opt in to treating an empty query result as "nothing to do"
- [ ] #2 The default behaviour (fail, retry, dead-letter) is unchanged and documented
- [ ] #3 Annotation processor test and an integration test cover both behaviours
<!-- AC:END -->
