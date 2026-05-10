---
id: TASK-188
title: Investigate full-suite test failure and hidden mongo module failure
status: In Progress
assignee:
  - copilot
created_date: '2026-05-10 11:34'
updated_date: '2026-05-10 12:32'
labels:
  - testing
  - rca
  - mongo
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Perform a root cause analysis for tests that fail only when running the full Maven test suite, including potential downstream failure in the mongo module. Reproduce deterministically, isolate the interaction causing failure, identify underlying defect, and propose/implement a fix with verification.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Failure is reproduced from a clean baseline using full-suite execution.
- [ ] #2 Primary failing test root cause is identified with concrete evidence (logs, stack traces, ordering/resource interaction).
- [ ] #3 Potential downstream mongo module failure is reproduced or ruled out with evidence.
- [ ] #4 A minimal, targeted fix is implemented for the underlying cause (no band-aid).
- [ ] #5 Relevant automated tests pass after fix, including a suite run that previously failed.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Reproduced full-suite failure with `mvn -DskipITs test` and captured log in `/tmp/prefab-full-test.log` (exit 1).

Primary observed failure (sns-sqs-example): `BadSqlGrammarException` querying `prefab_outbox` because relation does not exist; tests fail with HTTP 500 on create endpoints.

Isolated `examples/sns-sqs` without `-am` passes, but `-am` reactor run fails; after `clean`, migration `examples/sns-sqs/target/classes/db/migration/V1__generated.sql` includes `prefab_outbox` and relation-missing error disappears, revealing a different failure.

After clean regeneration, sns-sqs fails in `ChannelSummaryIntegrationTest` due event-processing race/optimistic-lock path (`OptimisticLockingFailureException`, `No aggregates found`, `No value present`) leading to timeout on expected totals.

Fail-at-end full run (`mvn -fae`) confirms additional downstream failures: `pubsub-example` and `mongodb-example`.

MongoDB hidden failure confirmed independently (`mvn -pl examples/mongodb -am test` and test-only run): `CategoryStatsIntegrationTest` times out with empty results; in isolated run category creation log appears but no category-created publish/consume logs, indicating event propagation for stats setup does not occur in that path.

Refactored `test/src/main/java/be/appify/prefab/test/pubsub/PubSubTestAutoConfiguration.java` to remove static container startup in class initialization (`<clinit>`). The Pub/Sub emulator is now provided as a managed Spring bean and wiring uses `@TestConfiguration(proxyBeanMethods = false)`.

Added `spring.kafka.consumer.auto-offset-reset: earliest` to `examples/mongodb/src/test/resources/application-test.yml` to prevent projection consumers from missing startup-time events in MongoDB example tests.

Validation: `mvn -pl test test -DskipITs` passed after the Pub/Sub test auto-config refactor.

Validation attempt: `mvn -pl examples/pubsub -am -Dtest=MessageIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` now fails with direct Docker availability error (no `PubSubTestAutoConfiguration.<clinit>` anymore), showing the class-init bootstrap failure mode is removed.

Current blocker: Docker/Testcontainers availability is intermittent (`Could not find a valid Docker environment`) so full verification of pubsub/mongodb integration tests is currently blocked in this environment.
<!-- SECTION:NOTES:END -->
