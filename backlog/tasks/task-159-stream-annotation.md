---
id: TBD
title: "Add @Stream annotation for SSE and WebSocket endpoint generation"
status: "To Do"
priority: "High"
labels: ["feature", "annotation-processor", "requested-by:maestro"]
---

## Background / Problem Statement

Prefab currently generates only synchronous REST endpoints. Applications that
need to push real-time data to clients (token-by-token LLM output, live task
progress, live log tailing) have no Prefab-idiomatic path and must write custom
Spring MVC controllers outside the Prefab model.

The Maestro project (autonomous agent platform) works around this with a
hand-written `ChatController` that streams LLM tokens via SSE. The same pattern
is needed by any Prefab application that pushes live data.

## Proposed API

### New annotation: `@Stream`

**Package:** `be.appify.prefab.core.annotations.rest`
**Target:** `METHOD`
**Retention:** `SOURCE`

```java
@Stream(path = "/stream", event = "token")
public Stream<String> streamTokens() {
    return tokenService.streamForSession(id);
}
```

| Attribute          | Type        | Default     | Description                                                                                                                                                        |
|--------------------|-------------|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `path`             | `String`    | `"/stream"` | Path suffix appended after `/{id}`. E.g. `"/stream"` → `GET /sessions/{id}/stream`.                                                                                |
| `event`            | `String`    | `"message"` | SSE event name sent in the `event:` field of each frame.                                                                                                           |
| `heartbeatSeconds` | `int`       | `15`        | Interval between keepalive `event: ping` frames. `0` disables heartbeat.                                                                                           |
| `terminal`         | `String`    | `""`        | *(Push model only.)* Name of a `boolean` field on the event record. When `true`, the SSE stream is closed after the final frame. Empty string disables auto-close. |
| `security`         | `@Security` | `@Security` | Security settings.                                                                                                                                                 |

### Aggregate usage example (from Maestro)

```java
@Aggregate
@GetById
@AsyncCommit
public record ConversationSession(
        @Id Reference<ConversationSession> id,
        @Version long version,
        String title,
        SessionStatus status
) {
    @Create
    public static SessionStarted start(String title) {
        return new SessionStarted(Reference.create(), title);
    }

    @EventHandler
    public static ConversationSession onStarted(SessionStarted event) {
        return new ConversationSession(event.id(), 0L, event.title(), SessionStatus.ACTIVE);
    }

    /** Streams agent output tokens to the SSE client. */
    @Stream(path = "/stream", event = "token")
    public Stream<String> streamTokens() {
        return AgentTokenRegistry.streamFor(id);
    }
}
```

### Event-driven streaming (push model via `@EventHandler`)

A second, complementary usage pattern places `@Stream` on an `@EventHandler`
method. Instead of the method *providing* a blocking stream (pull), each time the
event handler fires for a given aggregate instance, the event payload is *pushed*
to any SSE client currently connected for that instance.

This is the idiomatic pattern when a Kafka event already carries the data to be
streamed (e.g. agent tokens emitted as individual events):

```java
// The domain event carrying one LLM output token
@Event(topic = "${topics.tokens.name}")
public record TokenEmitted(
        @PartitioningKey Reference<ConversationSession> sessionId,
        String token,
        boolean done   // true on the final token of a response
) { }

// The aggregate
@Aggregate
@AsyncCommit
@GetById
public record ConversationSession(
        @Id Reference<ConversationSession> id,
        @Version long version,
        String title,
        SessionStatus status
) {
    @Create
    public static SessionStarted start(String title) {
        return new SessionStarted(Reference.create(), title);
    }

    @EventHandler
    public static ConversationSession onStarted(SessionStarted event) {
        return new ConversationSession(event.id(), 0L, event.title(), SessionStatus.ACTIVE);
    }

    /**
     * Pushes each token to connected SSE clients as it arrives from Kafka.
     * The {@code terminal = "done"} attribute names a boolean field on the event;
     * when {@code true}, the stream is closed automatically after this event.
     */
    @EventHandler
    @ByReference(property = "sessionId")
    @Stream(path = "/stream", event = "token", terminal = "done")
    public ConversationSession onTokenEmitted(TokenEmitted event) {
        return new ConversationSession(id, version, title,
                event.done() ? SessionStatus.COMPLETED : status);
    }
}
```

#### How the push model works

The `SSE connect` endpoint (`GET /conversation-sessions/{id}/stream`) is
generated the same way as in the pull model, but instead of consuming a
`Stream<T>` it registers an `SseEmitter` in a generated
`ConversationSessionSseRegistry` (an in-memory `ConcurrentHashMap<String, SseEmitter>`
keyed by aggregate ID):

```java
// Generated SSE connect endpoint (push model):
@GetMapping(path = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@PathVariable String id) {
    service.getById(id);  // throws NotFoundException if aggregate absent
    var emitter = new SseEmitter(0L);
    sseRegistry.register(id, emitter);
    emitter.onCompletion(() -> sseRegistry.remove(id));
    emitter.onTimeout(() -> sseRegistry.remove(id));
    scheduleHeartbeat(emitter, 15);
    return emitter;
}
```

The generated `onTokenEmitted` service method is augmented to push to the
registry after processing the event:

```java
// Generated event handler service augmentation:
void handleTokenEmitted(TokenEmitted event) {
    // ... existing @ByReference update logic ...
    sseRegistry.findById(event.sessionId().id()).ifPresent(emitter -> {
        try {
            emitter.send(SseEmitter.event().name("token").data(event.token()));
            if (event.done()) {
                emitter.complete();  // terminal=true — close the stream
            }
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    });
}
```

#### `terminal` attribute

The `terminal` attribute names a **boolean field on the event record**. When
that field is `true`, the generated code calls `emitter.complete()` after
pushing the final event frame, cleanly closing the SSE connection.

If `terminal` is omitted, the stream stays open until the client disconnects
or the aggregate's `@Update` method transitions it to a terminal status
(integration with `@Job`'s `@JobTerminal` — see companion task).

#### Pull model vs push model — when to use each

| Scenario                                                                         | Recommended model                       |
|----------------------------------------------------------------------------------|-----------------------------------------|
| Data comes from a Kafka event already in the Prefab event flow                   | **Push** (`@EventHandler` + `@Stream`)  |
| Data comes from an in-process blocking source (e.g. iterator, subprocess stdout) | **Pull** (method returning `Stream<T>`) |
| Data comes from a reactive source (Project Reactor)                              | **Pull** (method returning `Flux<T>`)   |
| Agent token streaming driven by Kafka                                            | **Push**                                |
| File download / progressive computation                                          | **Pull**                                |

Both models share the same `@Stream` annotation and the same generated SSE
connect endpoint signature — only the data-delivery mechanism differs.

---

## Generated Artefacts

For the example above, the processor generates into `ConversationSessionController`:

```java
// In the generated ConversationSessionController:
@GetMapping(path = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@PathVariable String id) {
    var session = service.getById(id);  // throws NotFoundException if absent
    var emitter = new SseEmitter(0L);   // no timeout — client controls lifecycle
    Thread.ofVirtual().start(() -> {
        try (var tokens = session.streamTokens()) {
            tokens.forEach(token -> {
                try {
                    emitter.send(SseEmitter.event().name("token").data(token));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            });
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    scheduleHeartbeat(emitter, 15);
    return emitter;
}
```

### Heartbeat behaviour

A virtual-thread heartbeat scheduler sends `event: ping\ndata: {}\n\n` every
`heartbeatSeconds` seconds while the emitter is open, preventing proxies and
load balancers from closing idle connections. Cancelled automatically on
`emitter.complete()` or `emitter.completeWithError()`.

### Disconnect handling

The generated endpoint calls `emitter.onCompletion()` and `emitter.onTimeout()`
to clean up any resources held by the streaming method (e.g. closing the returned
`Stream<T>`). If the aggregate method returns a `java.io.Closeable` stream,
it is closed in both completion callbacks.

### Interaction with `@Job` (if both annotations are present)

When `@Stream` is placed on the same aggregate as `@Job` (see companion task),
the generator merges the two: the SSE endpoint streams `JobProgressEvent`
records as JSON until a `JobCompleted` or `JobFailed` terminal event is sent.

## WebSocket variant (future extension)

The same annotation could support WebSocket by setting `protocol = "ws"`.
This is out of scope for the initial implementation but the annotation attribute
space should reserve `protocol` to avoid a breaking change.

## Acceptance Criteria

- [ ] `@Stream` annotation defined in `prefab-core` with the attributes above (including `terminal`)
- [ ] `StreamPlugin` implements `PrefabPlugin`; registered in `META-INF/services`
- [ ] **Pull model**: `StreamPlugin.writeController()` generates the SSE endpoint for each `@Stream`-annotated method returning `Stream<T>` or `Flux<T>`
- [ ] **Push model**: `StreamPlugin.writeController()` generates the SSE connect endpoint + `{Aggregate}SseRegistry` for each `@Stream` on an `@EventHandler` method
- [ ] **Push model**: `StreamPlugin.writeService()` augments the generated event handler service method to push to the registry
- [ ] Generated SSE connect endpoint: `GET /{basePath}/{id}/{path}` → `text/event-stream` content type (both models)
- [ ] Pull model: generated method uses `Thread.ofVirtual()` to consume the `Stream<T>`
- [ ] Pull model: generated method uses `Flux.subscribe()` for `Flux<T>` return type
- [ ] Push model: `{Aggregate}SseRegistry` is a `ConcurrentHashMap`-backed `@Component` scoped to the aggregate type
- [ ] Push model: `terminal` attribute resolves to a `boolean` field on the event record; `emitter.complete()` called when `true`
- [ ] Processor error if `terminal` names a field that does not exist on the event record
- [ ] Processor error if `terminal` names a field that is not `boolean` / `Boolean`
- [ ] Both models: generated method uses `SseEmitter` with zero timeout; heartbeat scheduler started; `onCompletion` / `onTimeout` remove emitter from registry
- [ ] `@Security` attribute applied to `@PreAuthorize` on the generated SSE connect endpoint (both models)
- [ ] `StreamPlugin` registered in the Prefab developer guide's annotation reference table
- [ ] Unit test: pull model — `@Stream` on `Stream<String>` generates correct endpoint method signature
- [ ] Unit test: pull model — `@Stream` on `Stream<MyRecord>` generates correct JSON SSE data serialisation
- [ ] Unit test: push model — `@Stream` on `@EventHandler @ByReference` generates registry + service augmentation
- [ ] Unit test: push model — `terminal = "done"` generates `emitter.complete()` call when `event.done() == true`
- [ ] Integration test (pull model): SSE endpoint delivers streamed items to a test client in order
- [ ] Integration test (push model): Kafka event fires → SSE client receives pushed payload
- [ ] Integration test (push model): `terminal=true` on event → stream closed after final frame
- [ ] Integration test (both): heartbeat ping received after configured interval
- [ ] Integration test (both): client disconnect triggers emitter cleanup
- [ ] Prefab developer guide updated: section 4.2 REST Annotations (pull model + push model); section 7.x new feature guide with both examples

## Implementation Notes

- **Pull model**: the aggregate method must return `Stream<T>` (blocking) or
  `Flux<T>` (reactive). The generator detects the return type and picks the
  appropriate consumption strategy (virtual thread vs reactive subscribe).
- **Push model**: `@Stream` on an `@EventHandler` method is only valid when that
  method is also annotated with `@ByReference` or `@Multicast`. The registry key
  is the aggregate ID resolved from the event via the `@ByReference` property.
  `@Multicast` generates multi-key registry pushes (one per resolved aggregate).
- The `T` type pushed must be JSON-serialisable via Jackson (same constraint as
  `@Update` return types). For the push model, `T` is inferred from the event
  record's field named by `event` if it exists, or the full event record otherwise.
- Consider generating a typed SSE event record `{event}Event(T data)` for
  documentation purposes in the AsyncAPI plugin (applies to both models).

