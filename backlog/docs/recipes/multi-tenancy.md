# Multi-tenancy

**Budget:** 750 tokens. **Verified example:** [`@TenantId` model](../../../annotation-processor/src/test/resources/tenant/project/source/Project.java) and [tenant processor tests](../../../annotation-processor/src/test/java/be/appify/prefab/processor/tenant/TenantPluginTest.java).

Add one non-null `@TenantId` component to an aggregate. Provide exactly one `TenantContextProvider` bean so Prefab can populate the tenant on create and scope reads, updates and deletes to the current tenant.

**Rules**
- Tenant identity comes from the authenticated request context, not a client-writable aggregate field.
- Use one tenant component per aggregate.
- Test that a tenant cannot read or mutate another tenant's aggregate.

**Common mistake:** filtering only the list endpoint or accepting tenant IDs from request bodies. Tenant isolation applies to generated operations; use the provider as the trusted source.

Details: [multi-tenancy guide](../feature-guides.md#76-multi-tenancy), [`@TenantId`](../annotation-reference.md#tenantid).
