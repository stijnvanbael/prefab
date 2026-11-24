---
id: TASK-337.10
title: 'Showcase variant: Postgres + AWS SNS/SQS'
status: Done
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-24 09:00'
labels:
  - examples
  - sns-sqs
dependencies:
  - TASK-337.05
  - TASK-337.06
  - TASK-337.07
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 181000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add `examples/showcase-sns-sqs`: pom (prefab-sns-sqs instead of prefab-kafka), application.yml, `application-local.yml` for LocalStack (keep the README guidance of the old example), test config and `showcase-capabilities.yml`. Replaces `examples/sns-sqs`. Includes neither the streams nor the Avro slice (yet).

Run the full showcase suite. Known gaps to declare: STREAMS (no SNS/SQS streams backend), ORDERED_DELIVERY (`@PartitioningKey` is ignored — standard topics, no FIFO/message group ID), AVRO (payloads not self-describing, TASK-275). Every other failing feature is fixed (if small) or declared with a new backlog task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Module contains no Java sources
- [x] #2 Full showcase suite passes or skips with declared gaps, each linked to a backlog task
- [x] #3 Coverage matrix updated
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Gap task IDs to put in showcase-capabilities.yml: STREAMS → TASK-342, ORDERED_DELIVERY → TASK-340, AVRO → TASK-275.

Update from TASK-342: the SNS/SQS streams backend (`prefab-streams-sns-sqs`) exists, so this variant should include the streams slice and not declare STREAMS. Add `prefab-streams`, `prefab-streams-sns-sqs` and `prefab-streams-postgres` and set `showcase.streams.skip=false`; leave `showcase.kafka.streams.skip` true (the Kafka-native breakout topology stays out). The slice compiled and its topology tests passed on these dependencies in a scratch variant. `@PartitioningKey` events are now FIFO (TASK-340), so re-check whether ORDERED_DELIVERY is still a gap.

### Implementation notes

- `examples/showcase-sns-sqs` is configuration only: pom (`prefab-postgres`, `prefab-sns-sqs`, `prefab-streams`,
  `prefab-streams-sns-sqs`, `prefab-streams-postgres`; `showcase.streams.skip=false`, Kafka breakout and Avro stay
  off), `application.yml`, `application-local.yml` (security roles, LocalStack endpoint and `test` credentials),
  `application-test.yml` (`prefab.test.containers: true`, like the other variants since TASK-175.05),
  `showcase-capabilities.yml` and a README that keeps the old example's local-run guidance.
- Declared gaps: `AVRO` and `SCHEMA_REGISTRY_API` (TASK-275, payloads are not self-describing), and
  `KAFKA_STREAMS_BREAKOUT` (breakout hands over the Kafka Streams API, which the in-process engine does not have,
  decision-008, tracked with the backend in TASK-342). `STREAMS` is not a gap. `ORDERED_DELIVERY` is not declared
  either: `@PartitioningKey` events are FIFO since TASK-340. No showcase code uses
  `StreamProcessorContext.schedule`, so TASK-363 needs no declaration.
- `examples/sns-sqs` is not removed here; TASK-337.11 removes the old example modules.

**Verification**

CI runs the full showcase suite on the real stack (LocalStack + Postgres) and passes, with the declared gaps skipped
by `CapabilityGapsTest`. The author session had no Docker, so everything before the first green CI run was checked
locally: the variant compiles with the streams slice, the topology, capability and coverage-matrix tests pass, and
the application's auto-configuration was inspected with `--debug` against a local Postgres. In-memory mode is no
substitute here (no Streams on the in-memory bus, TASK-374).

**CI findings**

- The first CI run failed on a Maven Central connection reset (not this PR). The re-run failed in this module: all 64
  integration tests failed to load the application context.
- Cause, fixed in `SqsUtil`: SNS topic names may only contain letters, digits, hyphens and underscores, but the
  showcase events use topics such as `showcase.customer-registered`. `SqsUtil` sanitized queue names but not topic
  names, so creating the topic failed at startup. Kafka and Pub/Sub accept dots, and the old `examples/sns-sqs` used
  dot-free topic names, so nothing had hit this. Topic names are now sanitized in `ensureTopicExists`, which every
  producer, subscriber and the streams sink go through. Covered by `SqsUtilTest`; CI went green with this change.
- Rebasing on main brought the Pub/Sub variant's fixes for the shared showcase sources (uncategorized sales,
  Awaitility uncaught exceptions), which this variant needs as well because `SqsUtil` also rethrows after retries
  (TASK-373).
<!-- SECTION:NOTES:END -->
