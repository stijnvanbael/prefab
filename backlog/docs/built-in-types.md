# Built-in Types

**Part of the [Prefab Developer Guide](developer-guide.md)**

---

## `Reference<T>`

**Package:** `be.appify.prefab.core.service`

A typed wrapper around a string ID. Used as the `@Id` field and for cross-aggregate references.

```java
public record Reference<T>(@JsonValue String id) {
    public static <T> Reference<T> create()           // random UUID
    public static <T> Reference<T> fromId(String id)  // from known ID
}
```

**Usage:**
```java
@Id Reference<Order> id           // primary key
Reference<Customer> customer      // foreign key to Customer aggregate
```

**Database mapping:** Stored as `VARCHAR(36)` (UUID string).
**JSON mapping:** Serialized as a plain string (not an object).

---

## `Binary`

**Package:** `be.appify.prefab.core.domain`

Represents an uploaded file.

```java
public record Binary(
        String name,          // original filename
        String contentType,   // MIME type (e.g. "image/jpeg")
        File data             // temporary file on disk
) { }
```

**Usage:** Use as a field type and annotate with `@Download` to generate a download endpoint.
Combine with `@ContentType` and `@FileSize` for validation.

**REST mapping:** Multipart form upload. The processor generates a `multipart/form-data` controller
parameter and a `StorageService` call to persist the file.

---

## `AuditInfo`

**Package:** `be.appify.prefab.core.audit`

Convenience record grouping the four audit fields.

```java
public record AuditInfo(
        @CreatedAt Instant createdAt,
        @CreatedBy String createdBy,
        @LastModifiedAt Instant lastModifiedAt,
        @LastModifiedBy String lastModifiedBy
) { }
```

**Usage:** Declare as a single field on the aggregate instead of the four individual fields.

**Database mapping:** Four separate columns: `created_at`, `created_by`, `last_modified_at`,
`last_modified_by`.

---

## `Page<T>`

**Package:** `be.appify.prefab.core.spring`

The return type of `@GetList` endpoints. Implements `org.springframework.data.domain.Page<T>`.

```java
public record Page<T>(
        List<T> content,
        PageInfo page
) { }

record PageInfo(int size, int number, long totalElements, int totalPages) { }
```

**Query parameters accepted by generated list endpoints:**

| Parameter     | Type     | Default | Description                                 |
|---------------|----------|---------|---------------------------------------------|
| `page`        | `int`    | `0`     | Zero-based page number                      |
| `size`        | `int`    | `20`    | Page size                                   |
| Filter fields | `String` | —       | One parameter per `@Filter`-annotated field |

---

## `AuditContextProvider`

**Package:** `be.appify.prefab.core.audit`

Strategy interface for resolving the current user's identity.

```java
public interface AuditContextProvider {
    String currentUserId();
}
```

**Default:** `SystemAuditContextProvider` returns `"system"`.

**Override:** Declare a `@Bean` of type `AuditContextProvider`:

```java
@Bean
public AuditContextProvider auditContextProvider() {
    return () -> SecurityContextHolder.getContext()
        .getAuthentication().getName();
}
```

---

## `TenantContextProvider`

**Package:** `be.appify.prefab.core.tenant`

Strategy interface for resolving the current tenant ID.

```java
public interface TenantContextProvider {
    String currentTenantId();  // return null to disable tenant filtering
}
```

**Default:** No-op bean that returns `null` (no tenant filtering).

**Override:**

```java
@Component
public class JwtTenantContextProvider implements TenantContextProvider {
    @Override
    public String currentTenantId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaimAsString("organisation_id");
        }
        return null;
    }
}
```

---

## `PublishesEvents`

**Package:** `be.appify.prefab.core.domain`

Implement on an aggregate to gain the `publish(Object event)` convenience method.

```java
public interface PublishesEvents {
    default void publish(Object event) { ... }
}
```

Internally delegates to `DomainEventPublisher.getInstance()`. In a running Spring application,
`SpringDomainEventPublisher` registers itself as the active instance on startup (via
`DomainEventPublisher.setInstance(this)`) and unregisters itself on shutdown (via
`DomainEventPublisher.reset()`). This ensures no static state leaks between Spring context
lifecycles.

When no publisher is registered (e.g. in plain unit tests without Spring), `publish()` silently
does nothing — unless you explicitly install a `CapturingDomainEventPublisher` via the
`PublishedEventsExtension` (see [Feature Guides — Unit Testing Domain Events](feature-guides.md#712-unit-testing-domain-events)).

---

## `SerializationRegistry`

**Package:** `be.appify.prefab.core.util`

Spring `@Component` that maps topic names to their serialization format. Prefab generates a
`SerializationRegistryConfiguration` bean for each `@Event`-annotated type; you do not normally
interact with this directly.

---

## Exception Types

| Class                 | HTTP Status | Package                         |
|-----------------------|-------------|---------------------------------|
| `BadRequestException` | 400         | `be.appify.prefab.core.problem` |
| `NotFoundException`   | 404         | `be.appify.prefab.core.problem` |
| `ConflictException`   | 409         | `be.appify.prefab.core.problem` |

Prefab's generated services throw `NotFoundException` when an aggregate is not found by ID.

