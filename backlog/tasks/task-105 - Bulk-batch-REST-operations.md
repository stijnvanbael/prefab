---
id: TASK-105
title: Bulk / batch REST operations
status: To Do
assignee: []
created_date: '2026-04-09 15:27'
updated_date: '2026-04-24 06:57'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 127000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
B2B applications routinely need to create, update, or delete hundreds or thousands of records in a single API call – think importing a product catalogue, bulk-approving invoices, or mass-archiving old records. Handling each item in a separate HTTP request is impractical at that scale.

Prefab should support a `bulk = true` attribute (or dedicated `@BulkCreate` / `@BulkUpdate` / `@BulkDelete` annotations) that generates a single endpoint accepting a list of items and processing them in a transaction, returning a structured result for each item (success or error).

Example usage:

```java
@Aggregate
public record Product(
    @Id Reference<Product> id,
    @Version long version,
    String sku,
    String name,
    BigDecimal price
) {
    @Create(bulk = true)   // POST /products/bulk  – body: [{sku, name, price}, ...]
    public Product(String sku, String name, BigDecimal price) { ... }

    @Update(bulk = true)   // PUT /products/bulk   – body: [{id, name, price}, ...]
    public void update(String name, BigDecimal price) { ... }

    @Delete(bulk = true)   // DELETE /products/bulk?ids=1,2,3
    public void delete() { ... }
}
```

The generated bulk endpoint wraps all items in a single transaction by default. An optional `transactional = false` attribute enables best-effort mode where each item is processed independently so that partial success is possible and the response reports per-item outcome.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add bulk = true attribute to @Create, @Update, and @Delete annotations (defaults to false); alternatively introduce @BulkCreate / @BulkUpdate / @BulkDelete if a separate annotation is preferred
- [ ] #2 The annotation processor generates a separate bulk controller method for each bulk-annotated operation following the naming convention POST /${resource}/bulk, PUT /${resource}/bulk, DELETE /${resource}/bulk
- [ ] #3 The generated bulk endpoint accepts a JSON array of the same request record used by the single-item variant and returns a BulkResult<T> containing a list of BulkItemResult<T> entries (each with an index, status SUCCESS or ERROR, the created/updated response object on success, and an error message on failure)
- [ ] #4 All items are processed within a single @Transactional boundary by default; a transactional = false attribute disables this so individual item failures do not roll back other items
- [ ] #5 The bulk endpoint is covered by the same @Security rules (roles, etc.) as the corresponding single-item operation
- [ ] #6 Add BulkResult and BulkItemResult classes to prefab-core
- [ ] #7 Generate OpenAPI documentation for the bulk endpoints, including the request-array schema and the BulkResult response schema
- [ ] #8 Add annotation-processor unit tests for bulk generate following the pattern of existing plugin tests
- [ ] #9 Add an example or integration test that exercises a bulk-create and bulk-delete endpoint end-to-end
<!-- AC:END -->
