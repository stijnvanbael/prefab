---
id: TASK-363
title: Wall-clock punctuation (StreamProcessorContext.schedule) in the in-process streams engine
status: To Do
assignee: []
created_date: '2026-11-21 10:00'
labels:
  - streams
  - coverage-gap
dependencies:
  - TASK-360
  - TASK-361
priority: low
type: feature
ordinal: 200000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`StreamProcessorContext.schedule(interval, task)` runs a wall-clock punctuation on Kafka once per task (partition), over that partition's stores. In the in-process engine (decision-008), state is shared by all instances, so running the task on every instance would duplicate its output. The first engine version rejects `schedule` at startup.

Support it by running each scheduled task on one instance at a time, behind a database lease per processor. Each run happens in one transaction, and its output goes through the outbox. Document the semantic difference: one run over all keys instead of one run per partition.
<!-- SECTION:DESCRIPTION:END -->

## Investigation

<!-- SECTION:ANALYSIS:BEGIN -->
### Current behavior

- Kafka delegates `schedule` to `WALL_CLOCK_TIME` on the processor task; each task's state store is a shard.
  Wall-clock punctuation is best effort, starts after an interval and can skip missed intervals. See
  [Kafka ProcessingContext](https://kafka.apache.org/40/javadoc/org/apache/kafka/streams/processor/api/ProcessingContext.html).
- The in-process engine has shared, database-backed state and no partition assignment. Pub/Sub ordering keys and
  SQS FIFO message groups provide ordering, not a stable worker for every key (decision-008). The engine's
  `repartition` and `keyBy` are logical operations, not broker repartition topics.
- `StreamProcessorContext.schedule` currently fails during initialization. `Store` exposes only `get`, `put`
  and `delete` through `put(key, null)`; it has no iteration or partition-aware access. A callback can access a
  known key or an application-maintained index, but neither backend currently offers “all keys in my partition.”
- The engine runs a processor instance against shared state, and only message delivery binds an active transaction
  and a thread-local processor context. A scheduled invocation needs its own context binding, transaction and
  downstream execution starting after the scheduling processor. It must not invent an input record or inherit
  unrelated message headers.

### Partition-style ownership

**Recommendation for this task:** treat each scheduled registration as one logical work partition owned by one
engine instance at a time. This is partition-style *ownership*, not Kafka-compatible key partitioning. Keep the
existing callback and shared-store behavior: one successful committed invocation for the processor's scheduled
occurrence, regardless of instance count. Independent processors can be owned by different instances.

Creating N virtual partitions for the existing callback is not safe: `schedule` has no partition argument and the
store has no partition-scoped scan, so N invocations can all operate on the same keys or emit the same output. A
real sharded alternative requires a separately designed API that exposes partition identity and restricts state
access, a stable serialized-key hash and fixed partition count, and a plan for re-keying, ownership, rebalance,
resharding and rolling upgrades. It would also require aligning input processing and punctuation ownership or
explicitly defining concurrency against the shared store. This is a broader change than TASK-363 and conflicts
with decision-008's rejection of recreating Kafka's broker partition protocol. Revisit it only with a demonstrated
need for parallel per-key scans; do not silently run existing callbacks once per virtual partition.

### Candidate implementation contract

- Give each topology, processor and scheduled registration a stable identity across instances and restarts.
  Multiple processors of the same class and multiple registrations must not collide. The current generic
  `process` stage name and declaration-order-derived names do not establish this contract; define behavior for
  topology changes, rolling upgrades and dynamic `schedule` calls from `process` before choosing an identity.
- Coordinate via a database-backed owner lease/fencing token for each registration. Acquire or renew atomically;
  validate the current owner, fencing generation and validity **in the same database commit** as state and outbox
  writes. A callback paused past its lease must not commit after another owner takes over. Use an authoritative
  database time or explicitly bound clock skew. The existing outbox relay lease is topic-specific and does not
  fence processor transactions, so it cannot simply be reused as-is.
- Persist a stable occurrence identity and atomically mark it complete with state changes and downstream outbox
  entries. This prevents a second owner from *committing* the same occurrence after a crash or an ambiguous commit.
  Existing Postgres and Mongo repositories buffer reads/writes before their short commit transaction, so add an
  explicit conditional fencing/occurrence guard at commit, not just a pre-callback lease check. The in-memory,
  Postgres and Mongo implementations need the same contract.
- At-least-one committed run per interval is impossible during arbitrary outages, and callbacks may execute more
  than once after failure or optimistic conflict even if only one run commits. Define startup, retry, missed ticks
  and failover semantics explicitly; Kafka skips missed ticks, while a durable catch-up policy would be different.
  External side effects inside callbacks are not rolled back; the outbox itself can publish again if a relay dies
  after broker acceptance. Guarantee atomic state and outbox insertion, not globally exactly-once side effects.
- On a scheduler thread, bind `ContextualStreamProcessor` and the engine transaction, provide a punctuation
  context with no input metadata, execute every forwarded record through the actual downstream graph once per
  branch, then commit and wake the outbox relay. Define callback timestamp and tracing headers explicitly. Ensure
  callbacks and ordinary processing have well-defined thread-safety and conflict/retry behavior.
- Inject a clock and scheduler so the in-memory topology harness can advance time deterministically without sleeps.
  `TopologySession` currently has no backend-neutral time-advance method; Kafka's driver is only accessible by
  `unwrap`. Decide whether to add a common API or a backend-specific facility, and avoid changing event time when
  advancing wall-clock time.

### Acceptance and verification implications

AC #1 should distinguish **one committed occurrence** from callback execution attempts, describe the policy for
missed intervals, and define the availability assumptions rather than promise execution during an outage. Preserve
the current ACs pending that decision. Add deterministic tests for two instances, independent registrations,
overlap and fencing after lease expiry, crash before/after commit, conflict retries, duplicate outbox delivery,
downstream branching, stop/restart, and time advances. Exercise the shared repository contract on in-memory,
Postgres and Mongo, and document the Kafka semantic differences and any required schema/configuration changes.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A scheduled task runs on exactly one instance per interval, proven with two engine instances
- [ ] #2 Output of a scheduled task is published through the outbox
- [ ] #3 The topology test harness can advance wall-clock time to trigger scheduled tasks
- [ ] #4 The decision-008 operator matrix is updated
<!-- AC:END -->
