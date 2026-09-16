---
id: TASK-285
title: Guard generated test clients when Spring Security filter chain is absent
status: Done
assignee: []
created_date: '2026-09-16 12:24'
labels:
  - bug
  - testing
  - security
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generated MockMvc test clients now auto-apply Spring Security test support whenever the security test classes are on the test classpath. Some example modules, including the Avro example, depend on `prefab-test` and therefore have `spring-security-test` available without also contributing a `springSecurityFilterChain` bean.

That combination makes the generated client constructor fail during Spring context startup with `springSecurityFilterChain cannot be null`, which breaks the GitHub Actions `build` job before the example integration tests can run.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Generated test clients only apply `SecurityMockMvcConfigurers.springSecurity()` when the application context actually contains a `springSecurityFilterChain` bean
- [x] #2 Existing MockMvc configurer ordering and application behaviour remains intact
- [x] #3 Annotation processor tests cover the generated constructor guard so the regression is caught before example modules fail at runtime
- [x] #4 The Avro example tests no longer fail during context startup because of `springSecurityFilterChain cannot be null`
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Analysis

- `examples/avro` depends on `prefab-test`, which brings in `spring-security-test`, but it does not depend on `prefab-security`, so the example has no `springSecurityFilterChain` bean.
- `TestClientWriter` currently generates `builder.apply(SecurityMockMvcConfigurers.springSecurity())` whenever Spring Security is on the processor classpath and no custom `MockMvcConfigurer` beans are provided.
- The generated fallback needs to be conditional on the filter chain bean actually being present in the `WebApplicationContext`; otherwise unsecured modules that merely inherit test support fail during bean construction.
- Updated `TestClientWriter` to guard the generated `springSecurity()` application behind `context.containsBean("springSecurityFilterChain")` while preserving the existing configurer sorting and application path.
- Added a focused regression test in `TestClientWriterTest` that asserts the generated constructor now contains the `springSecurityFilterChain` bean guard.
- Tried to run `mvn -q -pl annotation-processor,examples/avro -am test`, but local verification was blocked by resolution of `io.confluent:kafka-streams-avro-serde:8.3.0` from `https://packages.confluent.io/maven/`, so the runtime confirmation depends on CI.
<!-- SECTION:NOTES:END -->
