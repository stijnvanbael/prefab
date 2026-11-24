# Endpoint security

**Budget:** 750 tokens. **Verified example:** [secured Product operations](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/catalog/Product.java) and [security integration tests](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/ShowcaseSecurityAndDocumentationIntegrationTest.java).

Set `security = @Security(role = "...")` or `security = @Security(authority = "...")` on the specific REST operation that needs a rule. Configure authentication and authorization through the `prefab-security` module and Spring Security.

**Rules**
- Use roles for role checks and remember Spring's `ROLE_` authority prefix; use `authority` for an exact authority.
- Test anonymous access, an authenticated user without permission, and an authorized user.
- For a public endpoint, use the documented `HttpSecurityCustomizer` hook; do not add a competing filter chain.

**Common mistake:** assuming `@Security(enabled = false)` makes an endpoint publicly reachable through the filter chain. It removes method authorization only; configure the chain through the supported hook.

Details: [security guide](../feature-guides.md#719-security-and-api-documentation), [`@Security`](../annotation-reference.md#security).
