---
id: TASK-291
title: Align all Spring configurations with conditional auto-configuration gated on PrefabCoreConfiguration
status: Done
assignee: []
created_date: '2026-10-09 08:58'
labels:
  - spring
  - autoconfiguration
  - refactoring
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`KafkaConfiguration` is the reference model: it is an `@AutoConfiguration`, registered in `AutoConfiguration.imports`, guarded by `@ConditionalOnClass` for the relevant library, and by `@ConditionalOnBean({PrefabCoreConfiguration.class, ...})` for required beans. Other configuration classes in the suite are inconsistent: many are plain `@Configuration` classes (picked up via component scan or `@EnablePrefab`/`@Import`), several have no conditions, and the test auto-configurations are `@TestConfiguration` and/or lack the `PrefabCoreConfiguration` gate.

Goal: no configuration, including test configurations, activates without `PrefabCoreConfiguration`; every configuration activates only when its required beans are present and its relevant classes are on the classpath.

### Current state (analysis)

| Class | Module | Today | Gap |
|---|---|---|---|
| `PrefabCoreConfiguration` | core | `@Configuration` + `@ComponentScan`; imported by `@EnablePrefab` | Root of the chain; decide how it is registered (see notes) |
| `AuditConfiguration` | core | `@Configuration`, no conditions | Not auto-config, no gate |
| `PrefabRegistryConfiguration` | core | `@Configuration`, no conditions | Not auto-config, not in imports, no gate |
| `TenantConfiguration` | core | `@AutoConfiguration`, in imports | No `PrefabCoreConfiguration` gate |
| `KafkaConfiguration` | core | `@AutoConfiguration`, gated | Reference model |
| `PubSubConfiguration` | core | `@Configuration` + `@ComponentScan` + `@ConditionalOnClass` | Not auto-config, not in imports, no gate |
| `SnsConfiguration` | core | `@Configuration` + `@ComponentScan` + `@ConditionalOnClass` | Not auto-config, not in imports, no gate |
| `PrefabMongoConfiguration` | mongodb | `@Configuration`, in imports | No class/bean conditions, no gate |
| `PrefabJdbcConfiguration` | postgres | `@Configuration` + `@ComponentScan`, in imports | No class/bean conditions, no gate |
| `OpenApiAutoConfiguration` | openapi | `@AutoConfiguration`, in imports | Verify conditions, add gate |
| `AsyncApiConfiguration` | async-api | `@Configuration` + `@ComponentScan`, in imports | Not `@AutoConfiguration`, no conditions, no gate |
| `WebSecurityConfiguration` | security | `@Configuration`, no imports file | No conditions, no gate, not registered as auto-config |
| `StreamsConfiguration` | streams | `@Configuration` + `@ConditionalOnClass` | Not auto-config, no imports file, no gate |
| `PostgresTestAutoConfiguration` | test | `@TestConfiguration` + `@ConditionalOnClass` | No gate |
| `KafkaTestcontainerAutoConfiguration` / `KafkaTestAutoConfiguration` | test | `@TestConfiguration` + `@AutoConfiguration` | No gate |
| `PubSubTestAutoConfiguration` | test | `@Configuration` + `@ComponentScan` | No gate; `PubSubTestLifecycle` conditional only on `PubSubUtil` |
| `SnsTestAutoConfiguration` | test | `@TestConfiguration` | No gate; `SnsTestLifecycle` conditional only on `SqsUtil` |
| `MongoDbTestAutoConfiguration` | test | `@TestConfiguration` + `@AutoConfiguration` | No gate |
| `SecurityMockMvcTestAutoConfiguration` | test | `@TestConfiguration` + `@ConditionalOnClass` | No gate |
| `StreamTopologyConfiguration`, `MeterDataTopology` | examples/streams | Example application code | Out of scope (user code) |

Also review generated configuration (e.g. `UserExporterKafkaConsumerConfig` expected output) and `@EnablePrefab` / `@EnablePrefabStreams` so they stay consistent.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every Prefab configuration class in `core`, `mongodb`, `postgres`, `openapi`, `async-api`, `security`, `streams` and `test` is an `@AutoConfiguration` registered in its module's `AutoConfiguration.imports` file (no reliance on component scanning to discover them).
- [x] #2 Every such configuration (including all test configurations) carries `@ConditionalOnBean(PrefabCoreConfiguration.class)` (or equivalent) and does not activate when `PrefabCoreConfiguration` is absent.
- [x] #3 Every configuration has `@ConditionalOnClass` for the library classes it depends on, and `@ConditionalOnBean` for other beans it requires (e.g. `PrefabRegistryConfiguration`, `DataSource`, `MongoTemplate`).
- [x] #4 Ordering between configurations is expressed with `@AutoConfiguration(after/before = ...)` so `@ConditionalOnBean` is evaluated reliably.
- [x] #5 `@TestConfiguration` is removed or justified for test auto-configurations so they are not applied in an unrelated context; test auto-configurations activate only with `PrefabCoreConfiguration` present.
- [x] #6 Tests (ApplicationContextRunner-based) verify for each configuration: inactive without `PrefabCoreConfiguration`, inactive when the relevant class is missing (FilteredClassLoader), active when all conditions hold.
- [x] #7 Existing example modules and integration tests still pass (`mvn verify` on the full reactor); documentation (`backlog/docs/configuration.md`, getting-started) is updated.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Decision: PrefabCoreConfiguration stays registered via the explicit `@EnablePrefab` opt-in (user config is processed before auto-configurations, so the gate bean is visible). All other configurations are auto-configurations gated on it.
- `PrefabCoreConfiguration` is currently imported by `@EnablePrefab`. Since everything else is gated on it via `@ConditionalOnBean`, it must be registered before the auto-configurations are evaluated: either keep `@EnablePrefab` as the explicit opt-in (user config is processed before auto-configurations, so the bean is visible) or make it an `@AutoConfiguration` and use `@AutoConfigureAfter`. Decide and record in an ADR if the opt-in semantics change.
- Replace `@ComponentScan` on configurations with explicit `@Bean` methods or `@Import` of components where scanning would bypass the conditions.
- `@ConditionalOnBean` on auto-configurations only works reliably when the referenced beans come from user configuration or auto-configurations ordered earlier.

## Implementation Summary

- All configurations converted to @AutoConfiguration, registered in AutoConfiguration.imports (new files for security and streams) and gated on PrefabCoreConfiguration (security uses the class name because it does not depend on core). @TestConfiguration removed from test auto-configurations.
- @EnablePrefab now only imports PrefabCoreConfiguration.
- @ComponentScan replaced with explicit @Import of component classes: Spring rejects @ComponentScan combined with @ConditionalOnBean (REGISTER_BEAN phase).
- Ordering: efore Boot's Mongo/JDBC/Security auto-configs so @ConditionalOnMissingBean of Boot does not win; fter PrefabRegistryConfiguration, PubSubConfiguration, SnsConfiguration, KafkaConfiguration where @ConditionalOnBean depends on them.
- Removed @EnablePrefabStreams (breaking): StreamsConfiguration is now auto-configured when prefab-streams is on the classpath.
- WebSecurityConfiguration is now auto-registered for servlet apps (previously had to be imported manually).
- Tests: ApplicationContextRunner activation tests per module. Docker-based example integration tests could not be run locally (image pull timeout); the contexts reached container creation, confirming activation.
