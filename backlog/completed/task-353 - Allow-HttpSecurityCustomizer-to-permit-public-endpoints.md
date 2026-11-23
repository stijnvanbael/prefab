---
id: TASK-353
title: Allow HttpSecurityCustomizer to permit public endpoints
status: Done
assignee: []
created_date: '2026-11-21 09:45'
labels:
  - security
  - bug
  - agents
dependencies: []
priority: high
type: bug
ordinal: 198000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The annotation reference shows how to open endpoints with an `HttpSecurityCustomizer`:
`http.authorizeHttpRequests(auth -> auth.requestMatchers(...).permitAll())`. This fails at startup with `IllegalStateException: Can't configure requestMatchers after anyRequest`, because `WebSecurityConfiguration` registers `anyRequest().authenticated()` before it applies the customizers.

`@Security(enabled = false)` only removes `@PreAuthorize`; the filter chain still requires authentication. The only working option today is to replace the whole `SecurityFilterChain` bean, which means copying Prefab's defaults. Found by benchmark task `security-public-endpoints` (TASK-343).

The first benchmark baseline found a worse trap. Haiku and Sonnet both added a second, path-scoped
`SecurityFilterChain` (`securityMatchers(GET /articles/**)` + `permitAll()`), which is the usual Spring way. Because
Prefab's default chain is `@ConditionalOnMissingBean`, it silently disappears. Then no chain handles the other
requests, bearer tokens are never processed, and every authenticated write returns 401. Both agents' own tests only
checked anonymous requests, so they reported success.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Endpoints can be made public through HttpSecurityCustomizer (or an equivalent documented mechanism) without replacing the SecurityFilterChain
- [x] #2 Generated endpoints with @Security(enabled = false) are reachable anonymously, or the docs state clearly what that attribute does
- [x] #3 The documented example works and is covered by a test
- [x] #4 The benchmark task security-public-endpoints reference uses the documented mechanism
- [x] #5 Declaring an additional, path-scoped SecurityFilterChain does not silently remove Prefab's default chain, or Prefab fails at startup with an actionable message
<!-- AC:END -->

## Implementation Notes

## Analysis

- `WebSecurityConfiguration` currently adds `anyRequest().authenticated()` before invoking ordered customizers, so Spring Security rejects later custom `requestMatchers(...)`.
- `@Security(enabled = false)` removes generated method-level authorization only; the filter chain still requires authentication unless a matching request is explicitly permitted. The feature guide already says this, and the reference example should reinforce it.
- The default chain currently backs off when any `SecurityFilterChain` bean exists. Give Prefab's default chain a stable bean name and back off only when a replacement uses that name, so an additional path-scoped chain cannot silently remove the catch-all chain.
- Reorder default authorization after customizers, re-enable the showcase anonymous-listing integration test, and update the benchmark reference to use `HttpSecurityCustomizer` while keeping writes authenticated.

## Implementation

- Applied ordered `HttpSecurityCustomizer` beans before the default `anyRequest().authenticated()` rule, so `permitAll()` matchers can be added safely.
- Named the default chain `prefabSecurityFilterChain` and made auto-configuration back off only for an explicitly named full replacement. Added a context test proving an additional path-scoped chain leaves the default catch-all chain registered.
- Added an anonymous `GET /categories` matcher to the showcase and re-enabled its generated-client integration test. Updated security guides and the benchmark reference to use method-specific public GET matchers while writes remain protected.

## Verification

- `mvn -q -pl security -am -Dtest=WebSecurityConfigurationTest,WebSecurityConfigurationActivationTest,WebSecurityConfigurationAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test` passed.
- `mvn -q -pl examples/showcase -am -Dtest=CategoryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` passed (2 tests).
- The benchmark reference's hidden `PublicArticlesAcceptanceTest` passed (2 tests). The benchmark validator then reported a Windows workspace-cleanup failure, so its overall validation command did not complete successfully.
