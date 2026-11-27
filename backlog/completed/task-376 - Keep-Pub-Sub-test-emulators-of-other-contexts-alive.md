---
id: TASK-376
title: Keep Pub/Sub test emulators of other contexts alive
status: Done
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
- [x] #1 Two cached containers contexts in one JVM can both publish to and consume from Pub/Sub, covered by a test
- [x] #2 A Pub/Sub publish in tests fails with a clear error within a bounded time when the emulator is gone, instead of blocking forever
- [x] #3 The other container setups (Postgres, Kafka, LocalStack, MongoDB) are checked for the same problem
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
**Cause.** `PubSubTestAutoConfiguration#pubSubEmulatorContainer` named the emulator per application and force-removed
any container with that name before starting. Two cached contexts of one application therefore fought over one name:
the second context removed the running emulator of the first. Postgres, Kafka, LocalStack and MongoDB share one
container by design and use `removeStaleContainer`, which keeps a running container of the expected image.

**Decision: one emulator per context, not one shared emulator.** Topics and subscriptions are named after the
application, so contexts sharing an emulator would compete for the same subscription (messages delivered to the wrong
context) and the `@PreDestroy` clean-up of one context (`PubSubTestLifecycle`, deletes all topics) would break the others.

**Changes.**
- `TestContainerNameResolver.resolveUniqueContainerName` appends a random suffix to the resolved name; the Pub/Sub
  emulator uses it, so starting one never conflicts with or removes another. `removeConflictingContainer` had no other
  caller and is removed. Leftovers of a crashed JVM are removed by Testcontainers' resource reaper (`withReuse(false)`).
- Bounded publishing: the test support defaults `spring.cloud.gcp.pubsub.publisher.retry.total-timeout-seconds`,
  `initial-rpc-timeout-seconds` and `max-rpc-timeout-seconds` to 20/5/20 unless one of them is set. The three must be
  set together: the Pub/Sub client refuses a total timeout under 10 seconds, and Spring Cloud GCP fails the context when
  only some are set (max RPC timeout shorter than initial). Without a bound the publisher retries for ten minutes by default.
- `GenericPubSubPublisher` wraps a failed publish in a `CompletionException` naming the topic ("make sure Pub/Sub is
  available at the specified endpoint"), keeping the cause.

**Tests.** `TestContainerNameResolverTest`, `PubSubTestAutoConfigurationTest`, `GenericPubSubPublisherTest` (unit);
`PubSubPublishTimeoutTest` (a socket that accepts but never answers: fails after the 10 s bound; without the bound it
blocks past 100 s); `PubSubEmulatorSharingIntegrationTest` (Docker: two emulators for one application are both
running and both accept a publish). The Docker test could not be run in the session that implemented this (no Docker
daemon); it runs in CI.

**AC #3 findings.** Postgres, Kafka (and Schema Registry), LocalStack and MongoDB do not remove a running container of
the expected image, so two contexts in one JVM are fine **when Testcontainers reuse is enabled** (CI sets
`TESTCONTAINERS_REUSE_ENABLE=true`). With reuse disabled, `withReuse(true)` is ignored, the second context creates a
container with the same name and Docker answers 409. That is a different problem (no hang, a clear error) and is
tracked in TASK-404.
<!-- SECTION:NOTES:END -->
