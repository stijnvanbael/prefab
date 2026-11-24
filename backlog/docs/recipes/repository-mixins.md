# Repository mixins

**Budget:** 750 tokens. **Verified example:** [Product mixin](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/catalog/ProductRepositoryMixin.java) and [mixin integration test](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/catalog/ProductRepositoryMixinIntegrationTest.java).

Declare an interface annotated with `@RepositoryMixin(Aggregate.class)` to extend that aggregate's generated repository. Use a Spring Data derived method for a simple query, or `@Query` when the query needs explicit SQL or a custom aggregation.

**Rules**
- Keep the mixin in the application component scan.
- Use Prefab's `Reference<T>` domain type for aggregate references.
- Keep custom queries portable only when all configured persistence providers support them.

**Common mistake:** declaring a Spring Data repository for the aggregate. Put only additional methods in the mixin; Prefab generates and wires the base repository.

Details: [repository mixins](../feature-guides.md#710-repository-mixins), [extension points](../extension-points.md#82-repository-mixins).
