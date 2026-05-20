# Prefab Streams Example

Runnable Prefab Streams DSL example with breakout, branch-and-merge routing.

This module defines one topology that normalizes input, injects a native Kafka fragment, branches into two output paths, and merges back:

- `from(StreamEvent.class)` reads from `${topics.streams.input}`
- `breakout(new KafkaStreamBreakoutAdapter<>(...))` injects a native `KStream` fragment (`selectKey`)
- `branch(...)` routes short words (`<= 4`) to `${topics.streams.short-words}`
- `branch(...)` routes long words (`> 4`) to `${topics.streams.long-words}`
- `merge(...)` combines both branches and writes to `${topics.streams.words}`

## Commands

```bash
# Run tests for this module (and required upstream modules)
mvn -pl examples/streams -am test

# Run the example application
mvn -pl examples/streams -am spring-boot:run
```

