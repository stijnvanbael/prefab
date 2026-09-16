---
id: TASK-284
title: Customizable web security and role-aware @Security
status: To Do
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
- [ ] #1 `security` exposes a supported customisation extension point for the default `WebSecurityConfiguration` so adopters can contribute one or more customisers without replacing the generated `SecurityFilterChain` bean; with no customisers present, current default behaviour remains unchanged
- [ ] #2 `be.appify.prefab.core.annotations.rest.Security` adds an optional `role` attribute alongside the existing `authority` attribute, with Javadoc describing the intended Spring Security semantics
- [ ] #3 For every generated REST endpoint that already supports `security = @Security(...)`, `authority = "X"` is reflected in generated source as an authority-based authorisation rule and is covered by tests so the configured authority has an observable effect
- [ ] #4 `role = "ADMIN"` is reflected in generated source as a role-based authorisation rule using Spring Security role semantics, and generated test support creates matching mock authentication so secured endpoint tests remain realistic
- [ ] #5 If both `authority` and `role` are set on the same `@Security` declaration, annotation processing fails with a clear compiler error telling the user to choose exactly one of them
- [ ] #6 Add security-module tests for the web-security customiser hook and annotation-processor tests covering `enabled = false`, authority-based security, role-based security, and invalid mixed authority+role usage
- [ ] #7 Update the annotation reference and the REST security example module to show the supported `@Security` forms (`enabled`, `authority`, `role`) and how to extend `WebSecurityConfiguration` with customisers instead of replacing the full bean
<!-- AC:END -->
