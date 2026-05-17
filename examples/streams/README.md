# Prefab Streams Example

Runnable Prefab Streams DSL baseline example.

This module defines a topology that uses only source/sink operations:

- `from(StreamEvent.class)` reads from `${topics.streams.input}`
- `to("${topics.streams.output}")` writes to `${topics.streams.output}`

## Commands

```bash
# Run tests for this module (and required upstream modules)
mvn -pl examples/streams -am test

# Run the example application
mvn -pl examples/streams -am spring-boot:run
```

