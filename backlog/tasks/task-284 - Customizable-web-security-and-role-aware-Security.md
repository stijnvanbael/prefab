---
id: TASK-284
title: Customizable web security and role-aware @Security
status: Done
assignee: []
created_date: '2026-09-16 11:05'
labels:
  - security
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab's REST security contract is currently inconsistent in three places that matter to adopters:

1. The default `WebSecurityConfiguration` can only be changed by replacing the whole filter chain bean, which makes small framework-safe customisations unnecessarily heavy.
2. Setting `authority` on `@Security` is not reliably reflected in the generated REST source, so the public annotation contract does not match generated behaviour.
3. `@Security` has no first-class role attribute, forcing users to model role-based access through authorities only.

Prefab should provide an explicit customisation hook for the default web security chain and make `@Security` generate the expected authorisation rules for both authorities and roles across generated controllers, test scaffolding, documentation, and examples.

Out of scope:
- Replacing the existing default authentication model
- Introducing a broader policy DSL beyond `enabled`, `authority`, and a single optional `role`
- Supporting simultaneous `authority` and `role` on the same `@Security` declaration
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `security` exposes a supported customisation extension point for the default `WebSecurityConfiguration` so adopters can contribute one or more customisers without replacing the generated `SecurityFilterChain` bean; with no customisers present, current default behaviour remains unchanged
- [x] #2 `be.appify.prefab.core.annotations.rest.Security` adds an optional `role` attribute alongside the existing `authority` attribute, with Javadoc describing the intended Spring Security semantics
- [x] #3 For every generated REST endpoint that already supports `security = @Security(...)`, `authority = "X"` is reflected in generated source as an authority-based authorisation rule and is covered by tests so the configured authority has an observable effect
- [x] #4 `role = "ADMIN"` is reflected in generated source as a role-based authorisation rule using Spring Security role semantics, and generated test support creates matching mock authentication so secured endpoint tests remain realistic
- [x] #5 If both `authority` and `role` are set on the same `@Security` declaration, annotation processing fails with a clear compiler error telling the user to choose exactly one of them
- [x] #6 Add security-module tests for the web-security customiser hook and annotation-processor tests covering `enabled = false`, authority-based security, role-based security, and invalid mixed authority+role usage
- [x] #7 Update the annotation reference and the REST security example module to show the supported `@Security` forms (`enabled`, `authority`, `role`) and how to extend `WebSecurityConfiguration` with customisers instead of replacing the full bean
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Analysis

- `@Security` currently exposes only `enabled` and `authority`, while generator code in `ControllerUtil` already centralises both controller `@PreAuthorize` emission and generated MockMvc user setup. That makes `ControllerUtil` the right place to add shared role handling.
- The default `WebSecurityConfiguration` already hard-codes the filter chain. Existing Prefab code uses small callback interfaces plus `ObjectProvider#orderedStream()` for safe extension points, so the security module should follow that same pattern instead of introducing bean replacement guidance.
- There is no dedicated security generation test resource yet under `annotation-processor/src/test/resources/rest`, so this task will add a focused fixture that exercises `enabled = false`, `authority`, and `role` on generated REST endpoints plus invalid mixed usage.
- The security module currently has no unit tests. This task needs a small configuration-level test that proves customizers are optional and are applied when present.
- The docs are also inconsistent today: `annotation-reference.md` documents `authority`, while `feature-guides.md` still shows an older `authenticated` / `authorities` shape. The implementation needs to align those docs with the actual annotation contract.
- Implemented `@Security.role()` in `prefab-core`, kept `authority()` support, and added processor validation that rejects declarations setting both at once with a compiler error on the annotated element.
- Extended shared REST security generation in `ControllerUtil` so generated controllers now emit `hasAuthority(...)`, `hasRole(...)`, `permitAll()`, or `isAuthenticated()` as appropriate, and generated MockMvc clients build matching authority or role test users.
- Fixed a coupled gap in generated multipart/download test clients so endpoint security is respected there too, not just on JSON-based controller helpers.
- Added `HttpSecurityCustomizer` to the security module and wired `WebSecurityConfiguration` to apply all registered customizers after Prefab's defaults but before building the `SecurityFilterChain`.
- Added focused tests in `annotation-processor` and `security` to cover authority-based rules, role-based rules, disabled security, mixed authority+role rejection, and ordered security customizer application.
- Updated `annotation-reference.md`, corrected the outdated `feature-guides.md` example, and annotated the avro `Customer` example to demonstrate `enabled = false`, `authority`, and `role` usage in a real module.
- Verification completed with `mvn -q -pl core,annotation-processor,security -am test` and `mvn -q -pl examples/avro -am test-compile -DskipTests`. Example integration tests were intentionally skipped locally because this machine cannot run them reliably.
<!-- SECTION:NOTES:END -->
