# Testing with generated helpers

**Budget:** 750 tokens. **Verified example:** [Product generated-client integration tests](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/catalog/ProductIntegrationTest.java) and [test configuration](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/ShowcaseApplication.java).

Add `prefab-test` in test scope and annotate an integration test with `@IntegrationTest`. Prefab generates an `{Aggregate}Client` for HTTP operations; inject it to exercise the generated endpoints without hand-written request code. Tests use in-memory persistence and messaging by default.

**Rules**
- Use the generated client for endpoint behavior and plain JUnit tests for isolated domain logic.
- Assert observable API results, not generated implementation details.
- Set `containers = true` only when testing behavior that requires a real database or broker.

**Common mistake:** writing a parallel test controller or requiring Docker for every test. Use the generated helper and in-memory backend for the fast path.

Details: [integration tests](../feature-guides.md#720-integration-tests-in-memory-or-containers), [generated test helpers](../generated-artefacts.md#69-generated-test-client).
