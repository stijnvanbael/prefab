# Prefab Streams Example

Runnable Prefab Streams DSL example with breakout, branch-and-merge routing.

This module defines one topology that normalizes input, injects a native Kafka fragment, branches by subtype, and merges back via the `PrefabStreams` factory API:

- `from(StreamEvent.class)` reads from `${topics.streams.input}`
- `breakout(new KafkaStreamBreakoutAdapter<>(...))` injects a native `KStream` fragment (`selectKey`)
- `branch(ShortWordEvent.class)` emits short words (`<= 4`) to `${topics.streams.short-words}`
- `branch(LongWordEvent.class)` emits long words (`> 4`) to `${topics.streams.long-words}`
- `streams.merge(shortWords, longWords)` combines both subtype branches as `ClassifiedWordEvent` and writes to `${topics.streams.words}`

## Commands

```bash
# Run tests for this module (and required upstream modules)
mvn -pl examples/streams -am test

# Run the example application
mvn -pl examples/streams -am spring-boot:run
```

