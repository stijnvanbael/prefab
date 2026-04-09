---
id: TASK-106
title: Multi-tenancy support (@TenantId)
status: To Do
assignee: []
created_date: '2026-04-09 15:28'
labels:
  - "\U0001F4E6feature"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SaaS applications must isolate data per customer (tenant). Without framework support, every query, every insert, and every security check must be manually tenant-aware – an enormous and error-prone amount of boilerplate. Prefab should provide a `@TenantId` annotation that turns any String or Reference field into a tenant discriminator, generating all the filtering and population logic automatically.

Example usage:

```java
@Aggregate
@GetList
@GetById
public record Project(
    @Id Reference<Project> id,
    @Version long version,
    @TenantId String organisationId,   // automatically populated and filtered
    String name,
    String description
) {
    @Create
    public Project(String name, String description) { ... }
}
```

At runtime, a `TenantContextProvider` interface (single method `String currentTenantId()`) resolves the current tenant from the incoming request (e.g. JWT claim, header, or thread-local). The annotation processor generates:
- On every write: populate the `@TenantId` field from `TenantContextProvider`.
- On every read (GetById, GetList, Update, Delete): automatically add a `WHERE organisationId = :tenantId` predicate so tenants never see each other's data.
- A compiler error if a @TenantId field is included in any generated request record (it must remain server-side only).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add a @TenantId annotation to prefab-core (be.appify.prefab.core.annotations)
- [ ] #2 Add a TenantContextProvider interface to prefab-core with a single currentTenantId() method; provide a default no-op implementation that returns null (disabling tenant filtering) and log a warning if no custom bean is found
- [ ] #3 On write operations (create, update, delete) the generated service sets the @TenantId field to TenantContextProvider.currentTenantId() – the field must not appear in the generated request record
- [ ] #4 On read operations (GetById, GetList) the generated repository query includes a filter on the @TenantId column so only records belonging to the current tenant are returned; a cross-tenant read returns 404 not a forbidden error to avoid leaking aggregate existence
- [ ] #5 The generated Flyway migration includes a NOT NULL constraint on the @TenantId column and an index on it for query performance
- [ ] #6 The annotation processor raises a compile error if more than one @TenantId field is declared on an aggregate
- [ ] #7 A TenantConfiguration auto-configuration class registers the TenantContextProvider no-op bean if none is present
- [ ] #8 Add annotation-processor unit tests for the TenantId plugin following the pattern of existing plugin tests
- [ ] #9 README updated with a 'Multi-tenancy' section documenting the annotation, the TenantContextProvider contract, and a Spring Security JWT integration example
<!-- AC:END -->
