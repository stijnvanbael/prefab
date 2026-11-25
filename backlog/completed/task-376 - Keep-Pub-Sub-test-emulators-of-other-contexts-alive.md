---
id: TASK-376
title: Keep Pub/Sub test emulators of other contexts alive
status: To Do
assignee: []
created_date: '2026-11-24 08:30'
labels:
  - test
  - pubsub
dependencies: []
priority: high
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A Pub/Sub test suite hangs forever, instead of failing, when one JVM starts two Spring test contexts in containers
mode, for example two different `@IntegrationTest` configurations in one module.

`PubSubTestAutoConfiguration#pubSubEmulatorContainer` names the emulator with
`TestContainerNameResolver.resolveContainerName` (per application) and calls `removeConflictingContainer` first. That
force-removes **any** container with the same name, including the running emulator of the context that Spring's test
context cache still holds. Postgres, Kafka and LocalStack use `resolveSharedContainerName` with
`removeStaleContainer`, which keeps a running container of the expected image and avoids this.

The first context's next publish then blocks without a timeout (`pubSubTemplate.publish(...).join()`), so CI ran for
hours with no output. Found in TASK-175.05 (#229): `examples/pubsub` hung in `ChannelSummaryIntegrationTest` after
`MessageIntegrationTest` and `ChannelSummaryIntegrationTest` got their own context.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Two cached containers contexts in one JVM can both publish to and consume from Pub/Sub, covered by a test
- [ ] #2 A Pub/Sub publish in tests fails with a clear error within a bounded time when the emulator is gone, instead of blocking forever
- [ ] #3 The other container setups (Postgres, Kafka, LocalStack, MongoDB) are checked for the same problem
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
