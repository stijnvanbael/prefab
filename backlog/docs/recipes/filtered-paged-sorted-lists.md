# Filtered, paged and sorted lists

**Budget:** 750 tokens. **Verified example:** [Product aggregate](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/catalog/Product.java) and [list integration tests](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/catalog/ProductListIntegrationTest.java).

Add `@GetList` to the aggregate. Put `@Filter` on each queryable component; use the operator matching its type and intent. Set a default order with `@GetList(sorted = @SortOrder(property = "name"))`. The generated list accepts Spring `Pageable`, combines filters, and lets an explicit request sort override the default.

**Rules**
- Filter properties must be aggregate fields; use `Reference<T>` for aggregate references.
- Use range operators on comparable values and equality for exact matches.
- Verify page content, total count, and explicit-sort precedence in tests.

**Common mistake:** hand-building pagination or assuming a default sort overrides a client-supplied sort. Use generated list support and test both cases.

Details: [`@GetList` and `@Filter`](../annotation-reference.md#getlist), [feature guide](../feature-guides.md#71-rest-crud-operations).
