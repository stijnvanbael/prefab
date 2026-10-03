---
id: TASK-286
title: Compose default Spring Security MockMvc configurer with user-provided configurers
status: Done
assignee: []
created_date: '2026-09-21 13:20'
labels:
  - bug
  - testing
  - security
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generated test client constructors currently treat "apply the default Spring Security
MockMvc wiring" and "apply user-supplied `MockMvcConfigurer` beans" as mutually
exclusive:

```java
if (configurers.isEmpty() && context.containsBean("springSecurityFilterChain")) {
    builder.apply(SecurityMockMvcConfigurers.springSecurity());
} else {
    AnnotationAwareOrderComparator.sort(configurers);
    configurers.forEach(builder::apply);
}
```

As soon as a test author contributes any `MockMvcConfigurer` bean (e.g. to add a
logging filter or a custom result handler), the default Spring Security wiring is
silently dropped and they must reconfigure security from scratch. There is also no
supported way to override just the default security configurer while keeping the rest
of the default wiring.

Move the "apply Spring Security by default" decision into a proper Spring
auto-configuration in the `test` module so it composes with user configurers instead of
excluding them, and so it can be overridden using the standard
`@ConditionalOnMissingBean` idiom.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A new `SecurityMockMvcTestAutoConfiguration` in the `test` module contributes a `MockMvcConfigurer` bean named `securityMockMvcConfigurer` wrapping `SecurityMockMvcConfigurers.springSecurity()`, active only when Spring Security test classes are on the classpath and a `springSecurityFilterChain` bean is present
- [x] #2 Users can override the default security configurer by declaring their own `@Bean("securityMockMvcConfigurer") MockMvcConfigurer` bean, picked up via `@ConditionalOnMissingBean(name = "securityMockMvcConfigurer")`
- [x] #3 The generated test client constructor no longer special-cases security; it always sorts and applies the full list of `MockMvcConfigurer` beans, so the default security configurer composes with any other user-supplied configurers instead of being dropped
- [x] #4 The existing guard from TASK-285 (no failure when `spring-security-test` is on the classpath but no `springSecurityFilterChain` bean exists) is preserved via the auto-configuration's `@ConditionalOnBean` condition
- [x] #5 `TestClientWriterTest` and any generated-source fixtures are updated to reflect the simplified constructor; a new test proves a default security configurer bean and a user-supplied configurer are both applied together
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Added `be.appify.prefab.test.security.SecurityMockMvcTestAutoConfiguration` in the `test` module: a `@TestConfiguration` + `@ConditionalOnClass(SecurityMockMvcConfigurers.class)` class contributing a `securityMockMvcConfigurer` `MockMvcConfigurer` bean, guarded by `@ConditionalOnBean(name = "springSecurityFilterChain")` and `@ConditionalOnMissingBean(name = "securityMockMvcConfigurer")`. Registered it in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Simplified `TestClientWriter.buildConstructor()` (annotation-processor) to unconditionally sort and apply the full `List<MockMvcConfigurer>` — removed the `if (configurers.isEmpty() && context.containsBean(...))` special case entirely, along with the now-unused `SECURITY_MOCK_MVC_CONFIGURERS` import.
- Updated `TestClientWriterTest` to assert the new unconditional behaviour and added `SecurityMockMvcTestAutoConfigurationTest` (using Spring Boot's `ApplicationContextRunner`) covering: no bean without a filter chain, default bean registered when a filter chain bean is present, and a user-declared `securityMockMvcConfigurer` bean overriding the default. Note: `@ConditionalOnBean`/`@ConditionalOnMissingBean` are declaration-order sensitive with `ApplicationContextRunner.withUserConfiguration(...)`, so the filter chain / custom configurer configs must be listed before the auto-configuration class in the test — this mirrors how real auto-configurations are always processed after user `@Configuration` classes.
- Regenerated and refreshed the stale `annotation-processor/src/test/resources/rest/polymorphic/expected/ShapeClient.java` fixture (it wasn't actually asserted against by any test, but was out of date) to match current generated output, including the new `as(...)` support from TASK-287.
- Verified with `mvn -pl annotation-processor -am test` (392 tests) and `mvn -pl test -am test -DskipITs`, both green.
<!-- SECTION:NOTES:END -->
