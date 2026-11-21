---
id: task-008
title: Use parent parameter from path
status: To Do
assignee: []
created_date: '2025-10-10 13:33'
updated_date: '2026-11-21 18:10'
labels:
  - rest
dependencies: []
ordinal: 953.125
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A child aggregate with a `@Parent` reference is exposed under its parent, e.g. `ProductVariant` under
`/products/{productId}/product-variants`, but only create and list use the parent id from the path. Found while
building the showcase catalog (TASK-337.03):

- `GET /products/{productId}/product-variants/{id}` returns the variant whatever `productId` is: the generated
  controller does not even declare the `productId` path variable and calls `service.getById(id)`. A variant of product
  A is readable (and, the same way, updatable and deletable) under product B. It should be 404.
- The `Location` header of a created child is `/product-variants/{id}` instead of
  `/products/{productId}/product-variants/{id}`.

Apply the parent id to every operation on a child (get by id, update, delete, download, autocomplete where relevant)
and to the `Location` header, and cover it in the generated test client. Then add a test to
`examples/showcase/.../catalog/ProductVariantIntegrationTest` that a variant is not found under another product.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Get by id, update and delete of a child under another parent return 404
- [ ] #2 The Location header of a created child includes the parent path
- [ ] #3 The showcase covers a child requested under the wrong parent
<!-- AC:END -->
