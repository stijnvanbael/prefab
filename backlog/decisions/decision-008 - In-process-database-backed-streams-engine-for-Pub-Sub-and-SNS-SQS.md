---
id: decision-008
title: In-process, database-backed Prefab Streams engine for Pub/Sub and SNS/SQS
date: '2026-11-21'
status: accepted
related_tasks:
  - TASK-339
  - TASK-341
  - TASK-342
  - TASK-340
  - TASK-360
  - TASK-361
---

## Context

Since TASK-338, the Prefab Streams DSL (`prefab-streams`) has no Kafka dependency and `StreamBackend` can name any
backend. The only implementation is still `prefab-streams-kafka`. Applications on Google Pub/Sub or AWS SNS/SQS
cannot use the DSL, and the showcase variants for those brokers report STREAMS as a gap (TASK-341, TASK-342).

Kafka Streams relies on these Kafka features. Neither Pub/Sub nor SNS/SQS has them:

| Kafka Streams relies on | Pub/Sub | SNS/SQS |
|---|---|---|
| Partitions assigned to instances, so one instance owns a key | No partitions. Ordering keys give in-order delivery per key, but no guaranteed exclusive owner | No partitions. FIFO message groups are delivered in order and locked while a message of the group is in flight (TASK-340); standard queues have no ordering |
| Local state, rebuilt from changelog topics | No compacted topics and no replay beyond the retention window | No replay at all |
| Repartition topics, so a key moves to its owner | Possible with an extra topic, but there is no owner to move it to | Same |
| Stream time per partition, from record timestamps | Publish time per message | `SentTimestamp` per message |
| Exactly-once through Kafka transactions | At-least-once; exactly-once delivery is regional and does not cover output | At-least-once; FIFO deduplicates for five minutes |
| Global stores, by reading a topic from the beginning on every instance | No replay from the beginning | Same |
| Headers of any size | At most 100 attributes, values of at most 1024 bytes | At most 10 message attributes for SQS delivery |

Every Prefab application on these brokers already has a transactional database. All showcase variants for these
brokers run on Postgres.

### What the operators need

- **Stateless operators** (`filter`, `map`, `flatMap`, `branch`, `merge`, `as`, `to`, `end`, stateless `process`) only
  need a source and a sink. One subscription or queue per source and a publisher per sink are enough.
- **Keyed state** (`groupBy`+`aggregate`, `aggregate(groupBy, …)`, `StatefulStreamProcessor` stores, `deduplicate`)
  needs one consistent view per key and serialized updates to it. Kafka gets this from partition ownership. Without
  partition ownership, the store must be shared and must serialize updates itself.
- **Windows** (`windowedAggregate` with grace, `join` with `JoinWindow`) need event time, a notion of stream time to
  close windows, and cleanup of closed windows or join buffers.
- **Re-keying** (`keyBy`, `repartition`, `groupBy` with a new key) exists in Kafka so a key moves to the instance that
  owns its state. With a shared store there is no owner, so nothing has to move.
- **Global stores** need every instance to read all keys. A shared store meets this without replicating anything.
- **Exactly-once and deduplication** (TASK-215) need state updates and output to commit together with the
  acknowledgement of the input.
- **Tracing** (TASK-302) needs the message ID and correlation IDs to survive the broker's attribute limits.
- **`breakout`** hands the backend's native stream type to user code. A new backend has no native stream API to offer.

## Decision

1. **One broker-neutral engine, two broker adapters.**
   - `prefab-streams-engine` (TASK-360) implements `PrefabStreams` and `PrefabStream` by building an operator graph
     from the DSL and interpreting it in the application process. It defines two small SPIs:
     - `StreamSource` / `StreamSink`: subscribe with a per-message callback, and publish with key, headers and
       event time.
     - `StreamStateRepository`: key-value stores, window stores, join buffers, inbox and outbox.
   - `prefab-streams-pubsub` (TASK-341) and `prefab-streams-sns-sqs` (TASK-342) only provide sources, sinks, a
     `StreamBackend` constant (`pubsub`, `sns-sqs`) and auto-configuration. They reuse `PubSubUtil` and `SqsUtil` for
     topic, subscription, queue and dead-letter provisioning.
2. **State lives in the application's database. Postgres comes first.** State is stored in fixed `prefab_stream_*`
   tables: stores, windows, join buffers, inbox and outbox. Values are stored as bytes, written with the same
   serializers as broker messages (JSON or Avro). Keys use the shape-based key encoding of the Kafka backend, which
   moves to `prefab-streams` so both backends share it. Schema initialization works like Spring Batch's
   (`prefab.streams.state.initialize-schema=always|never`), and the DDL is published for teams that manage their own
   schema, so the engine never touches the application's Flyway version sequence. The Postgres repository is
   TASK-361. The MongoDB repository is TASK-362; it needs a replica set for multi-document transactions.
3. **One database transaction per input message.** For each delivered message the engine:
   1. checks and records the message in the inbox (`subscription`, message ID). A duplicate is acknowledged and
      skipped.
   2. runs the message through the operator graph, reading and writing state in the same transaction.
   3. writes every output record to the outbox in the same transaction.
   4. commits, then acknowledges (Pub/Sub ack, SQS delete). On failure it rolls back and the broker redelivers. The
      existing retry and dead-letter policies of `PubSubUtil` and `SqsUtil` apply unchanged.

   The message ID is `prefab-message-id` when present, otherwise the broker's message ID. The engine always sets
   `prefab-message-id` on its own output, so downstream inbox checks and SQS FIFO `MessageDeduplicationId` are
   reliable. As a result the engine is exactly-once with respect to its own state and output, whatever
   `prefab.streams.processing-guarantee` says. Both values are accepted. Side effects outside the database are not
   covered, as on Kafka.
4. **Optimistic concurrency guarantees correct state. Broker ordering only gives order.** Every state row has a
   version. A conflicting write rolls back the message transaction, and the engine retries it a bounded number of
   times before handing it to the broker's retry. This holds on any broker, any delivery order and any number of
   instances. It also covers stores whose key differs from the record key (as in `AggregationProcessor`).
   Per-key **order** comes from the broker:
   - On Pub/Sub, from ordering keys. `GenericPubSubPublisher` already sets them from `@PartitioningKey`, and
     subscriptions are created with message ordering enabled.
   - On SNS/SQS, from FIFO message groups (TASK-340).

   Sinks always publish with the record key as ordering key or message group. A stateful operator on an unordered
   source is allowed, and the engine logs a startup warning naming the source and operator. Its state stays correct,
   but an order-dependent aggregator can give a different result.
5. **Re-keying is logical.** `keyBy`, `repartition` and `groupBy` change the record key in memory and do not create
   a topic. Repartition names are still validated, must be unique and are used to name steps. The result is
   equivalent to Kafka's: after a repartition, the order of records with the same new key that came from different
   source keys was already arbitrary.
6. **Event time and stream time.**
   - **Event time** is the record timestamp the engine writes on its own output (`prefab-event-time`). Without it,
     the engine uses the broker's publish time (Pub/Sub `publishTime`, SQS `SentTimestamp`).
   - **Stream time is tracked per operator and key**, in the same row as that key's state, so no shared row becomes
     a hotspot. A record is late when its window end plus grace is before its key's stream time:
     - `windowedAggregate` drops late records, as Kafka does, and emits the updated aggregate after every record.
     - `join` drops a record older than the key's stream time minus time difference and grace, and emits one joined
       record per match within the time difference.
   - A scheduled cleanup deletes closed windows, join buffer entries and expired deduplication identities, using the
     highest stream time seen by each operator.
   - This deliberately differs from Kafka, which tracks stream time per partition. A record that Kafka drops because
     another key on the same partition advanced stream time is accepted here. Tests that rely on that cross-key
     effect are Kafka-specific.
7. **Global stores are shared tables, not replicas.** One shared subscription per global store (not one per
   instance) feeds a table that all instances read. A tombstone is a message with an empty body and attribute
   `prefab-tombstone=true`. Because neither broker can replay a topic from the beginning, a global store contains
   only events published after its subscription was created. The developer guide must say so.
   Bootstrapping from history (Pub/Sub seek to a snapshot or timestamp, or seeding from a query) is out of scope for
   the first version.
8. **The outbox keeps per-key output order.** A relay publishes outbox rows in sequence order and deletes them once
   the broker accepts them. One instance per sink topic holds the relay at a time, through a lease in the database,
   so two instances never publish the same key concurrently. The instance that commits wakes its own relay, so
   latency stays low. Gaps in the sequence (transactions that have not committed yet) do not matter: transactions
   for the same key are serialized (point 4), so their rows are in order. The Pub/Sub relay resumes an ordering key
   after a failed publish.
9. **Tracing.** `prefab-message-id`, `prefab-correlation-ids` and `prefab-event-time` are broker attributes.
   - Correlation IDs are kept in state rows, as Kafka's `TracedValue` does.
   - On the wire, the encoded correlation IDs are cut to the newest IDs that fit the broker limit (1024 bytes on
     Pub/Sub), so the "oldest evicted first" rule still holds.
   - On SNS/SQS, the engine uses at most three of the ten available message attributes.
10. **Unsupported in the first version, and a startup error when used:**
    - `breakout` with an adapter for any other backend. The engine has no native stream API, so a topology with a
      Kafka breakout cannot run on it. The showcase keeps its breakout topology in a Kafka-only configuration.
    - Wall-clock punctuation through `StreamProcessorContext.schedule`. With shared state it must run on one
      instance, behind a lease (TASK-363).

    The error message names the backend and the follow-up task.
11. **Tests stay backend-neutral.** `prefab-streams-engine` registers a `TopologyTestHarness.Factory` that runs the
    engine with an in-memory source, sink and state repository, synchronously and with deterministic time. A single
    repository contract test suite runs against both the in-memory and the Postgres repository, so topology tests
    that pass in memory also hold on Postgres.

### Operator support in the first version

| Operator / feature | Pub/Sub (TASK-341) | SNS/SQS (TASK-342) | Notes |
|---|:---:|:---:|---|
| `from`, `to(Class/EventType/String)`, `as`, `end` | ✅ | ✅ | One subscription or queue per source; `to(String)` creates the topic |
| `filter`, `map`, `flatMap`, `branch`, `merge`, `PrefabStreams.merge` | ✅ | ✅ | In memory, no state |
| `process` without stores | ✅ | ✅ | |
| `process` with stores (`StatefulStreamProcessor`), `aggregate(groupBy, …)` | ✅ | ✅ | Order-dependent logic needs ordered sources (SNS/SQS: TASK-340) |
| `StreamProcessorContext.schedule` | ❌ | ❌ | TASK-363 |
| `keyBy`, `repartition` | ✅ | ✅ | Logical, no topic |
| `groupBy` + `aggregate` | ✅ | ✅ | |
| `windowedAggregate` (tumbling, hopping, grace) | ✅ | ✅ | Stream time per key, see point 6 |
| `join` (inner, `JoinWindow`) | ✅ | ✅ | Join buffers in the database |
| `deduplicate` | ✅ | ✅ | Identities with retention in the database |
| `globalStore`, `globalStoreFrom(..).updatedBy(..)` | ✅ | ✅ | Shared table; no history before the first deployment |
| `breakout` | ❌ | ❌ | No native API; Kafka-only |
| Exactly-once / inbox deduplication | ✅ | ✅ | Inbox and outbox in the same transaction |
| Tracing (message and correlation IDs) | ✅ | ✅ | Correlation IDs cut to fit attribute limits |
| Topology tests through `TopologyTestHarness` | ✅ | ✅ | In memory |
| State on Postgres | ✅ | ✅ | TASK-361 |
| State on MongoDB | ✅ | ✅ | TASK-362, needs a replica set |

Most of this matrix comes from the engine. The broker tasks differ in ordering (FIFO on SNS/SQS needs TASK-340),
attribute limits and provisioning. SNS/SQS creates one queue per source, subscribed to the topic. FIFO throughput
quotas apply, and a failing message blocks its group until it moves to the dead-letter queue.

## Rejected alternatives

| Alternative | Why not |
|---|---|
| **Managed engine: Google Dataflow / Apache Beam** | The topology would be a separate pipeline deployment, not part of the Spring Boot application. DSL lambdas and Spring beans would have to be serializable and shipped to workers. Beam's windowing and triggering semantics differ from the DSL's. The application then runs on two runtimes and pays for a second service. A later Beam backend for very high volumes on Pub/Sub stays possible, because `StreamBackend` is open. |
| **Managed engine: AWS Managed Service for Apache Flink (formerly Kinesis Data Analytics)** | Same problems as Dataflow, and Flink does not consume SQS as a first-class source. In practice it means moving to Kinesis or MSK, which is a different broker. |
| **Run Kafka (or a Kafka-compatible broker) next to Pub/Sub or SNS/SQS** | This avoids the problem instead of solving it. Applications chose these brokers so they would not operate Kafka. |
| **In-process engine with local state per instance (RocksDB or in memory)** | Without partition assignment, an instance does not own its keys, so local state would be partial and inconsistent. Restarts would lose it, because nothing can be replayed to rebuild it. |
| **Partitioning built on top of the broker** (N topics or subscriptions per partition, plus assignment and leader election) | This rebuilds Kafka's group protocol and rebalancing inside Prefab: much more code, with weaker guarantees than the database already gives. |
| **Physical repartition topics** | Not needed for correctness with a shared, version-checked store (point 5). They would add latency, cost and provisioning. |
| **Pessimistic row locks instead of version checks** | `SELECT … FOR UPDATE` cannot lock a row that does not exist yet, so creating a new key would race anyway. Version checks work the same way on Postgres and MongoDB. |
| **Publish after commit, without an outbox** | A crash between commit and publish loses the output for good, because the inbox stops the input from being processed again. Publishing before commit creates duplicates and phantom output on rollback. |
| **Stream time per operator in one shared row** | Every record would update the same row and serialize all processing on it. |
| **Engine tables in the application's Flyway migrations** | Prefab generates and reconstructs `V{n}__*.sql` from the compile classpath. Library-owned migrations would interfere with that version sequence. |

## Consequences

- Pub/Sub and SNS/SQS applications can use the whole DSL except `breakout` and wall-clock punctuation. Most
  operators are written once, in the engine.
- Throughput is limited by the database: one transaction per message, plus outbox writes. That suits the event
  volumes of typical Prefab applications. Batching several messages per transaction is a possible later optimization,
  not part of the first version.
- Per-key order depends on the broker: ordering keys on Pub/Sub, and FIFO (TASK-340) on SNS/SQS. A poison message
  blocks its key until it moves to the dead letter queue, on both brokers.
- Semantics differ from the Kafka backend in documented ways: stream time per key, no global store history, no
  `breakout`. Topology tests written against the backend-neutral API run on both backends, except tests that rely on
  those differences.
- The showcase's native `breakout` topology must stay in a configuration that only loads with the Kafka backend, so
  that showcase-pubsub and showcase-sns-sqs can include the streams slice (TASK-337.06).
- Applications get `prefab_stream_*` tables in their database. The inbox and cleanup retention are configurable
  (`prefab.streams.inbox-retention`, default 7 days, matching Pub/Sub's default message retention).
- `PubSubUtil` and `SqsUtil` need a subscription variant whose consumer gets message metadata (message ID,
  attributes, publish time). Today they only pass the deserialized event.
