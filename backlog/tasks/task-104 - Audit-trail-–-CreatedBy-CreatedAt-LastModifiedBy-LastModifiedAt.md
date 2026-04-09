---
id: TASK-104
title: 'Audit trail – @CreatedBy, @CreatedAt, @LastModifiedBy, @LastModifiedAt'
status: To Do
assignee: []
created_date: '2026-04-09 15:27'
labels:
  - "\U0001F4E6feature"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Virtually every business application must record who created or last changed a record and exactly when. Compliance, debugging, and user-accountability all depend on this information. Prefab should let developers declare these four concerns with standard annotations directly on aggregate fields, then generate all the wiring needed to populate them automatically on every write operation.

The developer experience should be zero-boilerplate: annotate the field, configure the current-user resolver once, and the framework takes care of everything else.

Example usage:

```java
@Aggregate
@GetList
@GetById
public record Contract(
    @Id Reference<Contract> id,
    @Version long version,
    String title,
    @CreatedAt  Instant createdAt,
    @CreatedBy  String  createdBy,
    @LastModifiedAt Instant lastModifiedAt,
    @LastModifiedBy String  lastModifiedBy
) {
    @Create
    public Contract(String title) { ... }
}
```

The four annotations are placed on fields of the aggregate. The annotation processor detects them and generates the population logic so that:
- `@CreatedAt` is set to the current timestamp on creation and never overwritten.
- `@CreatedBy` is set to the identity of the authenticated principal on creation and never overwritten.
- `@LastModifiedAt` is updated to the current timestamp on every write (create and update).
- `@LastModifiedBy` is updated to the identity of the authenticated principal on every write.

A new `AuditContextProvider` interface (single method `String currentUserId()`) is generated/registered in prefab-core so projects can supply their own implementation (e.g. reading from Spring Security's `SecurityContextHolder`). A default no-op implementation is provided for unauthenticated scenarios.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add @CreatedAt, @CreatedBy, @LastModifiedAt, @LastModifiedBy annotations to prefab-core (be.appify.prefab.core.annotations.audit)
- [ ] #2 Add an AuditContextProvider interface to prefab-core with a single currentUserId() method and a default SystemAuditContextProvider no-op that returns 'system'
- [ ] #3 The annotation processor detects audit-annotated fields on aggregates and emits population code inside generated service methods (create/update) so that @CreatedAt/@CreatedBy are set once on creation and @LastModifiedAt/@LastModifiedBy are updated on every write
- [ ] #4 The four audit fields are included in the generated response record (e.g. ContractResponse) and are read-only – they must NOT appear in any generated request record
- [ ] #5 Generate a Flyway migration column for each audit field (TIMESTAMP WITH TIME ZONE for Instant, TEXT for String) when @DbMigration is present; apply the same for MongoDB (no migration needed, fields are just added to the document)
- [ ] #6 Add an AuditConfiguration @Configuration class (auto-configured) that registers the AuditContextProvider bean if none is present, following the same pattern as other auto-configurations in prefab-core
- [ ] #7 Add unit tests for the new annotation-processor plugin following the pattern of existing plugin tests (e.g. CreatePluginTest)
- [ ] #8 README updated with an 'Audit trail' section showing the annotations, the AuditContextProvider contract, and a Spring Security integration example
<!-- AC:END -->
