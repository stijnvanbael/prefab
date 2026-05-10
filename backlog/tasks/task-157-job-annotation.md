---
id: TBD
title: "Add @Job annotation for long-running operation support"
status: "To Do"
priority: "Medium"
labels: ["feature", "annotation-processor", "requested-by:maestro"]
---

## Background / Problem Statement

Prefab's `@AsyncCommit` pattern elegantly handles the "submit and return 202"
half of async operations, but provides no support for the second half: tracking
the progress of a long-running operation, polling its current state, or streaming
live progress events to a client.

Applications with long-running server-side work (batch jobs, AI agent tasks,
file processing pipelines, hardware design compilations) currently combine
`@AsyncCommit` with hand-rolled status aggregates and custom controllers, producing
significant boilerplate that duplicates the same polling and progress API pattern
every time.

The Maestro project implements this with a custom `TaskJournal` aggregate and a
`TaskJournal.appendStep()` update cycle. If `@Job` existed, this custom code
would be replaced entirely.

## Proposed API

### New annotation: `@Job`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`
**Retention:** `SOURCE`

Applied to an `@Aggregate` that also uses `@AsyncCommit`, `@Job` enriches the
generated service and controller with progress-tracking endpoints.

```java
@Aggregate
@AsyncCommit
@Job
@GetById
public record Task(
        @Id Reference<Task> id,
        @Version long version,
        String goal,
        TaskStatus status,
        @JobProgress int progressPercent,
        @JobLog @DbDocument List<String> logEntries
) {
    @Create
    public static TaskStarted start(@NotNull String goal) {
        return new TaskStarted(Reference.create(), goal);
    }

    @EventHandler
    public static Task onTaskStarted(TaskStarted event) {
        return new Task(event.id(), 0L, event.goal(), TaskStatus.RUNNING, 0, List.of());
    }

    @Update(path = "/progress")
    public Task recordProgress(int progressPercent, String logEntry) {
        return new Task(id, version, goal, status, progressPercent,
                Stream.concat(logEntries.stream(), Stream.of(logEntry)).toList());
    }

    @Update(path = "/complete")
    public Task complete() {
        return new Task(id, version, goal, TaskStatus.COMPLETED, 100, logEntries);
    }

    @Update(path = "/fail")
    public Task fail(String reason) {
        return new Task(id, version, goal, TaskStatus.FAILED, progressPercent,
                Stream.concat(logEntries.stream(), Stream.of("FAILED: " + reason)).toList());
    }
}
```

### New field annotation: `@JobProgress`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`

Marks the field that represents job completion progress (0–100). Must be `int`
or `Integer`. Used by the generated polling endpoint to populate the
`X-Job-Progress` response header and by the `@Stream` integration to emit
typed progress events.

### New field annotation: `@JobLog`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`

Optional. Marks a `List<String>` field as the structured job log. When present,
the generated SSE stream emits log entries as `event: log` frames in addition to
progress frames.

## Generated Artefacts

For the `Task` example above:

### 1. Enhanced `GET /{id}` response (from `@GetById`)

The generated `TaskResponse` includes `progressPercent` and the `X-Job-Progress`
header is set on the response:

```
HTTP/1.1 200 OK
X-Job-Progress: 42
Content-Type: application/json

{ "id": "...", "status": "RUNNING", "progressPercent": 42, ... }
```

### 2. New `GET /tasks/{id}/status` endpoint

A lightweight polling endpoint that returns only status and progress without the
full aggregate payload — suitable for frequent polling from a UI:

```
GET /tasks/{id}/status

HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "abc123",
  "status": "RUNNING",
  "progressPercent": 42,
  "terminal": false
}
```

`terminal: true` when `status` is one of the terminal values (determined by
the presence of terminal `@Update` methods annotated with `@JobTerminal`, or
by convention if the status enum has `COMPLETED` / `FAILED` values).

### 3. New field annotation: `@JobTerminal`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `METHOD`

Marks an `@Update` method as transitioning the job to a terminal state. The
generated status endpoint sets `terminal: true` when the aggregate's current
status was set by a `@JobTerminal` transition.

```java
@Update(path = "/complete")
@JobTerminal
public Task complete() { ... }

@Update(path = "/fail")
@JobTerminal
public Task fail(String reason) { ... }
```

### 4. Integration with `@Stream` (optional, when both present)

When `@Job` and `@Stream` are both present on the same aggregate, the processor
generates a built-in `GET /tasks/{id}/stream` SSE endpoint (no user-defined
method required) that:

- Polls the `@JobProgress` field on each `@Update` event via an internal
  `ApplicationEventListener`
- Emits `event: progress` frames: `{"progressPercent": 42, "status": "RUNNING"}`
- Emits `event: log` frames when `@JobLog` field changes (new entries only)
- Emits `event: complete` or `event: failed` terminal frame and closes the
  emitter when `@JobTerminal` transition fires

This removes the need for `@Stream` on a user-defined method for the standard
progress-tracking use case. The user-defined `@Stream` variant (see companion
task) is still available for custom streaming scenarios.

## Acceptance Criteria

- [ ] `@Job` annotation defined in `prefab-core`
- [ ] `@JobProgress` annotation defined in `prefab-core`; validated to target `int`/`Integer` fields only
- [ ] `@JobLog` annotation defined in `prefab-core`; validated to target `List<String>` fields only
- [ ] `@JobTerminal` annotation defined in `prefab-core`; validated to target `@Update` methods only
- [ ] `JobPlugin` implements `PrefabPlugin`; registered in `META-INF/services`
- [ ] `JobPlugin.writeController()` generates `GET /{id}/status` endpoint
- [ ] Generated status response record: `JobStatusResponse(String id, String status, int progressPercent, boolean terminal)`
- [ ] `X-Job-Progress` header set on generated `@GetById` response when `@JobProgress` field is present
- [ ] When `@Job` and `@Stream` both present: `GET /{id}/stream` SSE endpoint generated automatically (no user method required)
- [ ] SSE stream emits `progress`, `log` (if `@JobLog` present), `complete`, and `failed` event types
- [ ] SSE stream closes automatically on `@JobTerminal` transition
- [ ] Processor error `@Job requires @AsyncCommit` if `@Job` is present without `@AsyncCommit`
- [ ] Processor error `@Job requires exactly one @JobProgress field` if none or more than one `@JobProgress` is present
- [ ] Unit tests (annotation processor): status endpoint generated, `@JobTerminal` methods detected correctly
- [ ] Integration test: submit job → poll status → progress increments → terminal event received
- [ ] Integration test: SSE stream delivers progress events in order and closes on completion
- [ ] Prefab developer guide updated: `@Job`, `@JobProgress`, `@JobLog`, `@JobTerminal` in annotation reference; new feature guide section 7.x

## Implementation Notes

- The `@Job` + `@Stream` SSE integration works by subscribing to Spring
  `ApplicationEvent`s published by the generated `@Update` service methods
  (Prefab already publishes these). No additional plumbing is required in  
  user code.
- The `terminal` flag detection should use `@JobTerminal`-annotated methods
  as the authoritative source. Fallback heuristic (status enum values named
  `COMPLETED`, `FAILED`, `DONE`, `ERROR`) should be documented but not relied
  upon without `@JobTerminal`.
- Consider whether the status endpoint should be filtered by `@TenantId` when
  multi-tenancy is active — it should follow the same tenant filtering as
  `@GetById`.

