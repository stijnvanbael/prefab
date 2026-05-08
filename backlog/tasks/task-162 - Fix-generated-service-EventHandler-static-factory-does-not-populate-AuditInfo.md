---
id: TASK-162
title: >-
  Fix generated service: @EventHandler static factory does not populate
  AuditInfo
status: Done
assignee: []
created_date: ''
updated_date: '2026-05-08 05:31'
labels:
  - bug
  - annotation-processor
  - audit
  - 'reported-by:maestro'
dependencies: []
priority: high
---

## Problem Statement

When an `@Aggregate` uses `AuditInfo` (or the four individual `@CreatedAt` / `@CreatedBy` /
`@LastModifiedAt` / `@LastModifiedBy` fields) and has a static `@EventHandler` factory
method, the generated `onXxx()` service method calls `repository.save()` with the
aggregate returned directly from user code — **without first populating the audit fields**.

If the generated migration marks the audit columns `NOT NULL` (which it does by default),
this causes a `DataIntegrityViolationException`:

```
ERROR: null value in column "audit_created_at" of relation "conversation_session"
       violates not-null constraint
```

### Minimal reproduction

```java
@Aggregate
@GetById
public record ConversationSession(
        @Id Reference<ConversationSession> id,
        @Version long version,
        String title,
        AuditInfo audit          // ← four NOT NULL columns in the migration
) {
    @Create
    @AsyncCommit
    public static void start(String title) {
        publishEvent(new SessionStarted(Reference.create(), title));
    }

    @EventHandler
    public static ConversationSession onSessionStarted(SessionStarted event) {
        // AuditInfo cannot be meaningfully populated here — user passes null
        return new ConversationSession(event.id(), 0L, event.title(), null);
    }
}
```

The generated service:

```java
// Generated (broken for AuditInfo)
public void onSessionStarted(SessionStarted event) {
    conversationSessionRepository.save(
        ConversationSession.onSessionStarted(event));  // audit fields are null
}
```

### Current workaround (Maestro)

Populate `AuditInfo` manually in the `@EventHandler` with `Instant.now()` and `"system"`:

```java
@EventHandler
public static ConversationSession onSessionStarted(SessionStarted event) {
    var now = Instant.now();
    return new ConversationSession(
            event.id(), 0L, event.title(),
            new AuditInfo(now, "system", now, "system"));  // workaround
}
```

This is sub-optimal: the `"system"` actor is a hard-coded approximation, and the
timestamp is taken inside the handler rather than via the configured `AuditContextProvider`.

## Root Cause

The `AuditPlugin` correctly injects audit population code for `@Create` constructor paths
and `@Update` paths, but does not handle the static `@EventHandler` path inside
`StaticEventHandlerPlugin` / `onXxx()` service generation.

## Proposed Fix

In the generated `onXxx()` service method, apply the same audit injection logic as in
`@Create` service methods:

```java
// Fixed — generated service
public void onSessionStarted(SessionStarted event) {
    var now = Instant.now();
    var userId = auditContextProvider.currentUserId();
    var aggregate = ConversationSession.onSessionStarted(event);
    // Populate audit fields on the returned aggregate
    aggregate = new ConversationSession(
            aggregate.id(), aggregate.version(), aggregate.title(),
            new AuditInfo(now, userId, now, userId));
    conversationSessionRepository.save(aggregate);
}
```

Alternatively, apply this via a `AuditInfo.populate(AuditContextProvider, boolean isCreate)`
utility to keep the generated code DRY.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Static `@EventHandler` service methods invoke `AuditContextProvider` to populate audit fields before save
- [x] #2 Works for both `AuditInfo` field and individual `@CreatedAt` / `@CreatedBy` / ... fields
- [x] #3 `AuditContextProvider.currentUserId()` is called; falls back to `"system"` when the provider returns null/empty
- [x] #4 The generated migration audit columns remain `NOT NULL`
- [ ] #5 Integration test: session created via event handler has non-null `createdAt`, `createdBy`, etc.
- [x] #6 Workaround code (`new AuditInfo(now, "system", ...)`) can be removed from user `@EventHandler` methods
<!-- AC:END -->
