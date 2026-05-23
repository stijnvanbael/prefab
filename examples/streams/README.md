# Prefab Streams Example

Runnable Prefab Streams DSL example with breakout, branch-and-merge routing, and KStream-KStream join.

This module defines two topologies:

1) Branch-and-merge topology that normalizes input, injects a native Kafka fragment, branches by subtype, and merges back via the `PrefabStreams` factory API:

- `from(StreamEvent.class)` reads from `${topics.streams.input}`
- `breakout(new KafkaStreamBreakoutAdapter<>(...))` injects a native `KStream` fragment (`selectKey`)
- `branch(ShortWordEvent.class)` emits short words (`<= 4`) to `${topics.streams.short-words}`
- `branch(LongWordEvent.class)` emits long words (`> 4`) to `${topics.streams.long-words}`
- `streams.merge(shortWords, longWords)` combines both subtype branches as `ClassifiedWordEvent` and writes to `${topics.streams.words}`

2) Join topology that demonstrates KStream-KStream inner join with explicit `JoinWindow`:

- `from(JoinLeftEvent.class)` reads `${topics.streams.join-left}`
- `from(JoinRightEvent.class)` reads `${topics.streams.join-right}`
- both streams use deterministic Kafka keys via `@PartitioningKey` on `id`
- `join(right, JoinWindow.of(...), joiner)` emits `JoinedStreamEvent` to `${topics.streams.joined}` when keys match inside the window

## Commands

```bash
# Run tests for this module (and required upstream modules)
mvn -pl examples/streams -am test

# Run the example application
mvn -pl examples/streams -am spring-boot:run
```

