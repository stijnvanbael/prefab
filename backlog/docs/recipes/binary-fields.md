# Binary fields

**Budget:** 750 tokens. **Verified example:** [Product binary field and validation](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/catalog/Product.java) and [image integration test](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/catalog/ProductImageIntegrationTest.java).

Use `Binary` for file content. Annotate a field with `@Download` to expose its download endpoint; accept uploads through an annotated create or update operation. Apply `@ContentType` and `@FileSize` to enforce the upload contract.

**Rules**
- Multipart clients send the file under the generated operation parameter name.
- Set a download filename and content type when those are part of the contract.
- Verify upload, stored metadata and downloaded bytes together.

**Common mistake:** modeling a file as an arbitrary byte array or expecting JSON to carry multipart content. Use `Binary` and the generated multipart operation.

Details: [binary fields guide](../feature-guides.md#77-binary--file-fields), [`Binary` and `@Download`](../built-in-types.md#binary).
