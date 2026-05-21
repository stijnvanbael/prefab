---
id: TASK-224
title: Consolidate SerializationRegistry and EventRegistry into a single component
status: In Progress
assignee: []
created_date: '2026-05-21 08:52'
updated_date: '2026-05-21 13:49'
labels:
  - refactor
  - core
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Background

`SerializationRegistry` (`core/util`) and `EventRegistry` (`core/kafka`) serve overlapping purposes: both act as topic-keyed catalogues that are populated at startup and queried at runtime to drive serialisation/deserialisation decisions.

- **`SerializationRegistry`** maps a topic → `Event.Serialization` format (JSON, Avro, …) and is consumed by `SnsSerializer`, `SqsDeserializer`, and Kafka infrastructure.
- **`EventRegistry`** maps topic → Java type, type → topic(s), holds key-extractor functions, maintains the Jackson type-resolver allowlist, and implements `JacksonJsonTypeResolver`.

Both follow the same registry pattern (init-time registration, runtime lookup, customizer hooks) and are injected together in several places (e.g. `KafkaTestAutoConfiguration`, `KafkaPrefabStreamsTopologyTest`). Keeping them separate forces consumers to depend on two beans, duplicates the registry abstraction, and complicates the mental model.

## Goal

Merge the two registries into one cohesive component (e.g. `PrefabEventRegistry` or keep the name `EventRegistry`) that:

1. Retains every existing capability of both registries.
2. Exposes a single bean that consumers depend on.
3. Preserves or improves the customizer mechanism (`SerializationRegistryCustomizer` equivalents).
4. Keeps the public API backwards-compatible where possible, or provides a clear migration path.

## Files of Interest

- `core/src/main/java/be/appify/prefab/core/util/SerializationRegistry.java`
- `core/src/main/java/be/appify/prefab/core/util/SerializationRegistryCustomizer.java`
- `core/src/main/java/be/appify/prefab/core/kafka/EventRegistry.java`
- `core/src/main/java/be/appify/prefab/core/spring/EnablePrefab.java` (imports both)
- `test/src/main/java/be/appify/prefab/test/kafka/KafkaTestAutoConfiguration.java`
- `streams/src/main/java/be/appify/prefab/streams/kafka/StreamsConfiguration.java`
- All generated Kafka producer/registrar artefacts (annotation-processor templates)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A single registry component owns both topic→serialization-format and topic→type mappings, key extractors, and the Jackson type-resolver allowlist
- [ ] #2 SerializationRegistryCustomizer (or its successor) still allows third-party modules to register entries at startup
- [ ] #3 No consumer class needs to inject more than one registry bean
- [ ] #4 All existing unit and integration tests pass without modification (or are updated to reflect the new API)
- [ ] #5 The annotation processor generates registrars that target the consolidated component
- [ ] #6 EnablePrefab and auto-configuration wiring is updated to register only the new bean
- [ ] #7 Javadoc on the new component explains its full responsibility
- [ ] #8 Developer guide docs updated to reflect the consolidated component
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Analysis Findings

### How both registries are currently populated

| Registry | Population mechanism |
|---|---|
| `SerializationRegistry` | **Pull-based**: collects all `SerializationRegistryCustomizer` beans via `@Autowired(required=false) List<…>` and applies them in `afterPropertiesSet()`. The annotation processor generates one `@Bean`-returning-customizer per event package. |
| `EventRegistry` | **Push-based**: the annotation processor generates `*KafkaEventTypeRegistrar` `@Component` classes that receive `EventRegistry` via constructor injection and call `registerType(…)` in the constructor body. Spring's dependency graph guarantees the registry exists first. |

### Bean-collision risk vectors

#### 1. Double-registration from `@ComponentScan` + `@Import`
`KafkaConfiguration` has `@ComponentScan(basePackageClasses = EventRegistry.class)`, which picks up `EventRegistry` via classpath scanning. `EnablePrefab` explicitly `@Import`s `SerializationRegistry.class`. A naively merged `@Component` placed in the Kafka package and also imported by `EnablePrefab` would cause Spring to register the same class twice — though Spring deduplicates `@Component` + `@Import` of the exact same class, it is fragile and breaks if the class moves.

**Fix:** define the consolidated registry as a `@Bean` inside a `@Configuration` class (e.g. `PrefabRegistryConfiguration`) annotated with `@ConditionalOnMissingBean`. Remove the `@Component` stereotype from the registry itself. `EnablePrefab` then `@Import`s the configuration class, not the bean class directly.

#### 2. Customizer interface mismatch after the merge
`SerializationRegistryCustomizer.customize(SerializationRegistry)` takes the old concrete type. After the merge every generated customizer would need to accept the new unified type. If the interface is updated but old customizer beans compiled against the old interface remain on the classpath, Spring will silently skip them.

**Fix:** update `SerializationRegistryCustomizer` (or rename to `EventRegistryCustomizer`) to accept the consolidated type; bump the annotation-processor output; keep the old interface as a `@Deprecated` bridge during a transition window.

#### 3. Ordering — customizers applied before registrars run
`SerializationRegistry.afterPropertiesSet()` fires during the `SmartInitializingSingleton` phase. Constructor-injection registrars (the `*KafkaEventTypeRegistrar` pattern) fire during normal bean construction. If any bean reads from the registry during its own `@PostConstruct` or `afterPropertiesSet()` — before all registrars have been constructed — it may see an incomplete registry.

**Fix:** migrate everything to the customizer (pull-based) pattern: registrars become `EventRegistryCustomizer` beans that are collected and applied atomically inside `afterPropertiesSet()`, so consumers that read the registry after that phase always see a fully-populated snapshot.

### Recommended population strategy

```java
@Configuration
class PrefabRegistryConfiguration {

    @Bean
    @ConditionalOnMissingBean          // allows test configs to replace it
    PrefabEventRegistry prefabEventRegistry(
            List<EventRegistryCustomizer> customizers) {
        var registry = new PrefabEventRegistry();
        customizers.forEach(c -> c.customize(registry));   // atomic, ordered, no race
        return registry;
    }
}
```

- A **single `@Bean` definition** in a known configuration class eliminates the `@ComponentScan` + `@Import` collision.
- **`@ConditionalOnMissingBean`** lets test fixtures supply a pre-populated stub without conflict.
- Collecting all `EventRegistryCustomizer` beans as a constructor parameter guarantees the registry is fully populated before any other bean receives it — no partial reads possible.
- Generated `*KafkaEventTypeRegistrar` classes are refactored into `EventRegistryCustomizer` implementations, eliminating the push/pull split and the ordering ambiguity entirely.

### API design

The consolidated registry exposes a single overloaded `register` method. Type and serialization format are always known together at generation time; a single call prevents inconsistent intermediate state:

```java
// without partitioning key extractor
void register(String topic, Class<E> type, Event.Serialization serialization);

// with partitioning key extractor
void register(String topic, Class<E> type, Event.Serialization serialization, Function<E, String> keyExtractor);
```

### Example: generated EventRegistryCustomizer

The annotation processor generates one `@Component` per event package, replacing both the `*KafkaEventTypeRegistrar` and the `*SerializationRegistryConfiguration`:

```java
// Generated by Prefab annotation processor
package kafka.single.infrastructure.kafka;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.util.EventRegistryCustomizer;
import kafka.single.UserCreated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedEventRegistryCustomizer implements EventRegistryCustomizer {

    private final String topic;

    public UserCreatedEventRegistryCustomizer(
            @Value("${prefab.user.topic:prefab.user}") String topic) {
        this.topic = topic;
    }

    @Override
    public void customize(EventRegistry registry) {
        registry.register(topic, UserCreated.class, Event.Serialization.JSON, event -> event.user().id());
    }
}
```

Key points:
- The topic is injected via `@Value` so property-placeholder topics still work.
- `EventRegistryCustomizer` takes `EventRegistry` (the consolidated type); Spring collects all implementations and passes them to `PrefabRegistryConfiguration.prefabEventRegistry(List<EventRegistryCustomizer>)` before the bean is exposed to any consumer.
<!-- SECTION:NOTES:END -->
