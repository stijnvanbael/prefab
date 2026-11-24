# Audit fields

**Budget:** 750 tokens. **Verified example:** [Product audit component](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/catalog/Product.java), [audit integration test](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/catalog/ProductAuditIntegrationTest.java), and [principal provider](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/PrincipalAuditContextProvider.java).

Add `AuditInfo audit` to an aggregate to capture created/modified timestamps and actors. Prefab fills these values; the request must not choose them. Provide an `AuditContextProvider` when the actor should come from the application's authenticated principal.

**Rules**
- Use the aggregate's audit metadata in responses only when the API should expose it.
- Test both creation and update; creation fields must remain stable after updates.
- Ensure the provider returns a stable identity for the current actor.

**Common mistake:** setting audit fields in a constructor or update method. Let Prefab manage the metadata.

Details: [audit trail guide](../feature-guides.md#75-audit-trail), [`AuditInfo`](../built-in-types.md#auditinfo).
