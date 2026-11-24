---
id: TASK-363
title: Wall-clock punctuation (StreamProcessorContext.schedule) in the in-process streams engine
status: Done
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
`StreamProcessorContext.schedule(interval, task)` runs a wall-clock punctuation on Kafka once per task (partition), over that partition's stores. In the in-process engine (decision-008), state is shared by all instances, so running the task on every instance would duplicate its output.

Support each registration as a logical work partition assigned by rendezvous hashing across live application members. Rebalance when membership changes; use a database lease and fencing token to commit callback state and outbox output atomically. Document the semantic difference: one run over shared state instead of one run per key partition.
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

### Implementation decisions

- Implement membership-aware reassignment, not just opportunistic lease races. Members heartbeat into the shared
  state repository; each scheduled registration is assigned to one live member with rendezvous hashing. A joining
  member only moves registrations for which it wins the hash; a departed member's registrations move after its
  membership lease expires. All clocks used for leases must be within a documented bound.
- Use a persisted registration identity derived from the configured application/topology group, processor class,
  source bindings, stable declaration ordinal and normalized interval. Same-version replicas must build the same
  identities.
  Persist a topology fingerprint at member heartbeat; reject a changed declaration while another version remains
  live or an old schedule lease is active, then allow an incompatible version after the group is drained.
- Lease and fence an individual due occurrence, not merely a processor forever. Callback work runs once per
  occurrence at a time; the fencing generation is checked in the same repository transaction as its state and
  outbox writes. Callback retries after failure are allowed; duplicate committed output for one occurrence is not.
- Use Kafka-like best-effort wall-clock semantics: first run after one interval, skip missed ticks rather than burst
  catch-up, and make the next occurrence eligible on the next aligned interval. A task may be attempted again after
  owner failure; external side effects are outside the transaction guarantee.
- The test harness gets a controllable clock/advance hook. Punctuation has no input record metadata; forwards use a
  fresh message identity and continue through each downstream branch in the graph.
- Extend the repository contract for membership heartbeat/snapshot and schedule registration/claim/fenced commit,
  implementing it consistently for memory, Postgres and MongoDB. Add the schedule, member and group-fingerprint
  tables/collections through the existing idempotent engine schema initialization. No keyed state partitioning or
  store iteration is introduced.
<!-- SECTION:ANALYSIS:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Implemented wall-clock scheduling as one logical work partition per registration. Rendezvous hashing assigns each
  registration to one live member; member and schedule leases support reassignment, and fencing is checked atomically
  with state and outbox writes.
- Registration IDs include the normalized interval, so a changed interval creates a new registration. A topology
  fingerprint prevents mixed-version members from starting until the previous membership and callback leases drain.
- In-memory, Postgres and MongoDB repositories implement membership, schedule leases and fenced completion. Postgres
  DDL and MongoDB collection/index initialization include the new coordination records.
- Punctuation runs in a transaction-bound processor context, forwards through finalized downstream branches and
  commits outputs through the outbox. Missed intervals are skipped; the test harness advances a controllable wall
  clock.
- Updated the decision record, feature guide, cheat sheet and benchmark task. No generated artefact changed.
  `TASK-346` is still in progress on a separate branch; add or verify the Streams punctuation recipe when that recipe
  catalog is integrated.
- Post-rebase containerless stream tests pass for engine, Kafka, Pub/Sub and SNS/SQS, and the changed modules and tests
  compile. Postgres and MongoDB repository tests passed earlier with Testcontainers, but were excluded from the final
  containerless run. The benchmark reference acceptance test passes when run directly in its generated Maven
  workspace; the benchmark CLI cannot launch `mvn` as a child process on this Windows environment.
- The full reactor test remains blocked by a pre-existing Windows path failure in
  `AgentContextPluginTest.writesBelowTargetWhenOutputIsAFileSystemDirectory`. The benchmark module also has unrelated
  environment-sensitive failures in `IsolationTest` (Linux paths) and `ReportTest` (locale-specific decimal output).
<!-- SECTION:NOTES:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A scheduled task occurrence is committed by exactly one assigned member; membership joins and departures reassign registrations, proven with at least two engine instances
- [x] #2 Output of a scheduled task is published through the outbox
- [x] #3 The topology test harness can advance wall-clock time to trigger scheduled tasks
- [x] #4 The decision-008 operator matrix is updated
<!-- AC:END -->
