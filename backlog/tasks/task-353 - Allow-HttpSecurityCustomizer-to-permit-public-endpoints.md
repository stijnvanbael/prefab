---
id: TASK-353
title: Allow HttpSecurityCustomizer to permit public endpoints
status: To Do
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
- [ ] #1 Endpoints can be made public through HttpSecurityCustomizer (or an equivalent documented mechanism) without replacing the SecurityFilterChain
- [ ] #2 Generated endpoints with @Security(enabled = false) are reachable anonymously, or the docs state clearly what that attribute does
- [ ] #3 The documented example works and is covered by a test
- [ ] #4 The benchmark task security-public-endpoints reference uses the documented mechanism
- [ ] #5 Declaring an additional, path-scoped SecurityFilterChain does not silently remove Prefab's default chain, or Prefab fails at startup with an actionable message
<!-- AC:END -->

## Implementation Notes

The showcase's `CategoryIntegrationTest.findCategories_listsThemByName` uses a generated client that sends an
anonymous request for `@Security(enabled = false)`, but the default filter chain responds with 401. The test is
temporarily disabled until this task makes public endpoints reachable anonymously; re-enable it as part of the fix.
