# Aggregate with REST CRUD

**Budget:** 750 tokens. **Verified example:** [Product aggregate](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/catalog/Product.java) and [CRUD integration test](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/catalog/ProductIntegrationTest.java).

Annotate the record with `@Aggregate`, then add `@Create`, `@GetById`, `@GetList`, `@Update` and `@Delete` only for the operations this resource needs. Put create behavior in an annotated constructor and state changes in annotated methods that return the new record.

**Rules**
- Use `@Id Reference<Product>` and `@Version long version`; keep the aggregate immutable.
- Use Jakarta validation on constructor and operation parameters.
- Run `mvn test` to compile the model, generate endpoints and test the generated client.

**Common mistake:** writing a controller, DTO, repository or migration by hand. Add Prefab annotations to the aggregate instead; generated classes under `target/` are not source files.

Details: [REST CRUD reference](../feature-guides.md#71-rest-crud-operations), [generated artefacts](../generated-artefacts.md).
