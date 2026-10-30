---
id: TASK-303
title: Support generic event types in all event related features
status: In Progress
assignee: []
created_date: '2026-10-30 14:08'
updated_date: '2026-10-30 22:14'
labels:
  - events
  - generics
dependencies: []
references:
  - backlog/tasks/task-028 - Generic-parameters.md
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
As a developer modelling domain events, I want to declare events with type parameters (for example `record Changed<T>(Reference<T> id, T payload) implements Keyed<...>` or `Envelope<P>`), so that I can reuse a single event shape across aggregates without duplicating records per payload type.

Today event handling is keyed on the raw `Class` of the event. Parameterised events either fail at annotation processing time or lose their type arguments at runtime, so every event-related feature must be checked and made generic-aware.

### Features in scope
- Event publishing (`@Event`, topic resolution) and the event registry (`topicForType`, topic per concrete parameterisation).
- Event handlers (`@EventHandler`), including handlers declared for a concrete parameterisation (`Changed<Customer>`) and wildcard/supertype handlers.
- Serialization formats: JSON, Avro (schema generation, naming and registration for parameterised records), schema evolution and event versioning (TASK-274, TASK-301).
- Messaging platforms: Kafka, Pub/Sub, SNS/SQS, including topic/queue/subscription naming and provisioning.
- Streams: `PrefabStreams.from/to`, joins, aggregations, stores, the topology test API and message tracing (TASK-302).
- Generated artefacts: dispatch, subscribers, documentation and test fixtures from annotation processing.

### Concepts to settle in the analysis
- Event identity: how a parameterised event maps to a topic (one topic per erased type, or one per concrete parameterisation) and to a schema name.
- Runtime type capture: generic type information is erased, so the concrete parameterisation must be recoverable from handler signatures, stream definitions and published instances (for example via `ParameterizedType` or an explicit type token).
- Supported parameter forms: concrete type arguments only, or also wildcards and bounded parameters.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 An analysis documents how generic events are identified (topic, schema name, registry key) and records the decisions as an ADR.
- [x] #2 A parameterised event can be declared, published and handled for a concrete parameterisation with JSON serialization.
- [ ] #3 Avro schemas are generated and registered for parameterised events with deterministic, collision-free names.
- [x] #4 Event handlers for different parameterisations of the same event type receive only their own events.
- [ ] #5 Kafka, Pub/Sub and SNS/SQS publish and consume generic events, including topic/queue provisioning.
- [ ] #6 Prefab streams (`from`, `to`, `join`, `aggregate`, processors) and the topology test API support generic events.
- [ ] #7 Event versioning and schema evolution work for generic events.
- [ ] #8 Unsupported generic forms fail at build time with a clear annotation processing error.
- [ ] #9 Non-generic events keep their current behaviour, topics and wire format.
- [ ] #10 Documentation and feature guides describe generic events and their limits.
- [ ] #11 Repeatable `@Event` declarations bind ordered concrete `typeArguments` to their own topics: `Changed<Customer>` routes only to its customer topics and `Changed<Product>` only to its product topics, with separate registry entries, generated registrations and provisioning.
- [x] #12 `EventPublisher.bind(Class<E>)` supports non-generic events and `bind(EventType<E>)` captures generic bindings once; handles resolve the active publisher per call, legacy direct publishing is deprecated, and test capture exposes payloads rather than infrastructure envelopes.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Implementation progress
- Pub/Sub and SNS/SQS JSON integration now preserves concrete identity in attributes and original payload class metadata, resolves full JSON target types, and uses token-aware subscription requests. Generated subscribers create independent names for each owner/binding/resolved topic, including shared topics and property expressions; names use a full digest within SQS's 80-character limit. Ordinary subscription names are unchanged.
- Spring dispatch now offers a token-aware `canDispatch` overload, with a default bridge for custom dispatchers. Built-in dispatchers select the concrete binding's platform rather than claiming every parameterisation by raw class. Shared publication validation and event/component version guards are reused across all three JSON transports.
- Resuming transport integration: preserve concrete identity in Pub/Sub attributes and SNS notification attributes, add token-aware subscription requests, and generate independent subscriptions/queues for bindings sharing a topic. Reuse Kafka's version checks across all JSON transports; preserve existing class-based request APIs and non-generic wire contracts. Verify runtime payloads, subscription isolation, multi-topic dispatch and generated Java before committing.
- Kafka JSON slice: repeated declarations are discovered without handlers; concrete registrars, resolved member types, binding-specific object mothers and AsyncAPI components are generated. Kafka listeners use each binding's topics and canonical identity guard. JSON serialization writes only the payload plus identity/version headers, and deserialization resolves only topic-registered concrete tokens.
- Validation now runs before generation, with diagnostics for wrong argument counts, duplicate bindings, raw/primitive/array arguments and undeclared/raw/wildcard/type-variable handler bindings. Bound substitution handles dependent and self-referential bounds without recursive raw manifest expansion. Generic Avro and migration remain unsupported and must not be mistaken for completed support.
- Resuming with processor binding discovery/validation and Kafka JSON routing. Verify publisher-only repeated declarations, generated concrete registrations and handler topic isolation before extending the remaining Avro/transport/streams surfaces.
- Publication API revised with user approval: bind once with `EventPublisher.bind(CustomerCreated.class)` or `EventPublisher.bind(new EventType<Changed<Customer>>() {})`, then call `publisher.publish(event)`. Handles retain only immutable type information and resolve the active infrastructure publisher on each call. Deprecate legacy direct publication entry points without removing them; deprecation is a migration warning, not a compile-time prohibition.
- Initial inspection confirms repeatable annotations must be handled in processor discovery, per-binding generation, runtime version lookup and provisioning; changing `@Event` alone would silently hide repeated declarations from existing single-annotation readers.
- Bound-publisher implementation covers both overloads, immutable handles, null/raw-type rejection, explicit missing-infrastructure errors, deprecated legacy entry points, payload-preserving test capture and Spring dispatcher selection by payload class. Generic transport serialization and processor binding generation remain unfinished; the new handle alone does not complete AC #2-8 or #11.

### Analysis (current state)
- Event identity is currently erased to `Class<?>`: `EventRegistry` registers topics, serializers, reader schemas and dispatch strategies by raw classes; generated event registrars and consumers also resolve raw types.
- Annotation processing already receives generic `TypeMirror` values, but event handler discovery compares `asElement()` (the raw declaration), and generated event serializers/registrars are built from `TypeManifest`. Generic event declarations and parameterized handler signatures therefore need end-to-end type handling, not only a runtime registry change.
- JSON consumers need the concrete target type when binding a message. Kafka Avro additionally needs a parameterisation-specific reader schema; the existing reader-schema registry is keyed by `Class<?>`.
- `@Event` declares static topics. Provisioning and subscriber generation use those topic strings, so parameterising event types must not silently change existing non-generic topic names or generated artefacts.
- The scope spans `core`, `annotation-processor`, `avro-processor`, `kafka`, `pubsub`, `sns-sqs`, `streams`, topology tests, examples and backlog documentation. TASK-274 supplies evolution/reader-schema foundations; TASK-301 supplies version/migration rules; TASK-302 supplies stream tracing metadata and topology tests.

### Design direction for the ADR
- Introduce one canonical, immutable event type key that represents the raw event declaration plus recursively canonicalised concrete type arguments. Use this key for parameterised registry entries, reader schemas, handler routing and version/migration lookup; keep the existing raw-class path for non-generic events.
- Make `@Event` repeatable and add `Class<?>[] typeArguments() default {}`. Each declaration explicitly binds one concrete parameterisation to its declared topics; never combine topic sets across different parameterisations or derive broker names from Java type names. Retain the existing declaration shape for non-generic events.
- Capture generic handler signatures from annotation-processing `TypeMirror`s. Generic publication binds an explicit type token once in an `EventPublisher`; non-generic publication can bind a class. Do not guess type arguments from payload field values.
- Generate deterministic Avro schema names from the raw schema identity plus a stable encoding/hash of the canonical type key. Detect collisions at processing time; do not rename schemas or alter wire formats for existing non-generic events.
- Initially support concrete class arguments expressible through `typeArguments`, including generic field types resolved from those bindings. Reject raw generic events, unresolved type variables, wildcard bindings and unsupported bounds with element-specific processor errors; direct nested-argument bindings are outside this declaration API.
- The ADR must also decide how the type key appears in JSON/Avro payload metadata and transport headers/attributes, how Avro-on-Pub/Sub and SNS/SQS behave given TASK-275's lack of writer schemas, and whether event-version baselines/migrations are tracked per raw generic declaration or per concrete specialisation.

### Agreed declaration shape: one generic event, different topics per parameterisation

The following is the agreed API for TASK-303, now implemented for concrete JSON bindings on Kafka, Pub/Sub and SNS/SQS. Declarative provisioning and Avro remain in progress. The user selected repeatable `@Event` declarations with `typeArguments` rather than annotated type-token fields.

```java
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.PartitioningKey;
import org.springframework.stereotype.Component;

@Event(
        topic = "${topics.customer-changed:customer-changed}",
        typeArguments = Customer.class,
        platform = Event.Platform.KAFKA)
@Event(
        topic = "${topics.product-changed:product-changed}",
        typeArguments = Product.class,
        platform = Event.Platform.KAFKA)
record Changed<T>(@PartitioningKey String id, T payload) {}

record Customer(String name) {}
record Product(String sku) {}

@Component
class ChangeHandlers {
    @EventHandler
    public void onCustomerChanged(Changed<Customer> event) {}

    @EventHandler
    public void onProductChanged(Changed<Product> event) {}
}
```

| Concrete event type | Topic (default property value) | Handler |
|---|---|---|
| `Changed<Customer>` | `customer-changed` | `onCustomerChanged` |
| `Changed<Product>` | `product-changed` | `onProductChanged` |

- Each annotation's `typeArguments` is positional: for `Changed<K, V>`, use `typeArguments = {CustomerId.class, Customer.class}` to bind `K` and `V`. Validate argument count and declared bounds at build time.
- A binding can itself declare multiple topics using the existing `topic = {"primary", "audit"}` and `publishTo = PublishTo.ALL` syntax. That fan-out applies only to that parameterisation, never to another annotation's topics.
- The processor discovers both bindings even when the module has no handlers, generates separate typed registrations and provisions both configured topics. Handlers and registry lookups use the full concrete type, not `Changed.class`.
- Publishing retains `Changed<Customer>` or `Changed<Product>` identity through `EventPublisher.bind(new EventType<Changed<Customer>>() {})`. `new Changed<Customer>(...)` alone does not preserve that identity at runtime; do not infer it from the payload or fan out to both bindings. Non-generic events use `EventPublisher.bind(CustomerCreated.class)`. Both handles expose `publish(event)`; legacy unbound publication is deprecated.
- Reject missing/incorrect argument counts, bound violations, raw generic arguments, conflicting duplicate bindings and publications without a registered concrete binding explicitly. Preserve existing non-generic multi-topic semantics.
- Java annotation class literals cannot express `List<Customer>.class`. Consequently, this declaration API supports concrete non-generic class arguments, not direct bindings such as `Changed<List<Customer>>`; nested parameterized fields inside an otherwise concrete binding can still be resolved. Explicit nested-argument bindings require a future type-token declaration API rather than a string-based workaround.

### Agreed bound-publisher usage in aggregates

```java
@Aggregate
public record Customer(
        @Id Reference<Customer> id,
        @Version long version,
        String name
) {
    private static final EventPublisher<CustomerCreated> CREATED =
            EventPublisher.bind(CustomerCreated.class);
    private static final EventPublisher<Changed<Customer>> CHANGES =
            EventPublisher.bind(new EventType<Changed<Customer>>() {});

    @Create
    public Customer(String name) {
        this(Reference.create(), 0L, name);
        CREATED.publish(new CustomerCreated(id, name));
    }

    @Update
    public Customer rename(String name) {
        var updated = new Customer(id, version, name);
        CHANGES.publish(new Changed<>(id.toString(), updated));
        return updated;
    }
}
```

`CustomerCreated` is an ordinary non-generic event. No `PublishesEvents` implementation or
publisher dependency is needed in aggregate state. Binding is safe before startup and
requires no registry/broker access. Publishing requires an active `DomainEventPublisher`
(or the capturing test publisher) and fails explicitly if none is registered.
`CHANGES.publish(new Changed<Product>(...))` is a compile-time type error.
Legacy direct `publish(...)` calls remain available with deprecation warnings during
migration; this is not a compile-time ban. Concrete JSON routing is implemented on
Kafka, Pub/Sub and SNS/SQS; generic Avro, streams and migration remain unfinished.

### Implementation steps
1. **Settle contract and record ADR.** Record the agreed repeatable `@Event(typeArguments)` declaration and its class-literal limits. Confirm explicit publication/type-token API, metadata encoding, deterministic schema naming, duplicate/collision handling, version/evolution baseline identity, and transport limitations. Map each decision to acceptance criteria and retain current non-generic topic names and bytes.
2. **Add the canonical event type model.** Implement and unit-test the type-key representation, stable name/metadata encoding and equality semantics in `core`. Extend registry lookups for parameterised keys while preserving existing `Class<?>` overloads and behavior. Define explicit errors for missing or ambiguous generic identity.
3. **Teach annotation processing to preserve type arguments.** Add the repeatable `@Event` container and `typeArguments` attribute; update all annotation discovery, including dependency events, to read every binding rather than a single annotation. Update event and handler discovery, validation and generated dispatch/registrars to carry full `DeclaredType` arguments instead of reducing to `asElement()`. Add compile-testing fixtures for both bindings above, argument count/bounds, duplicate bindings, nested fields, raw, wildcard and unresolved forms; ensure unsupported forms fail with actionable diagnostics.
4. **Implement JSON publish/consume routing.** Carry the canonical key through event publication and transport metadata, bind JSON with the captured concrete target type, and select only handlers matching that key. Test two specialisations on the same topic, wildcard/supertype handlers, missing/unknown keys and non-generic regression behavior.
5. **Implement Avro specialization.** Substitute generic arguments into generated field schemas and converters, register the reader schema under the canonical key, and generate deterministic collision-checked Avro names. Add code-first tests and explicitly implement or reject generic `@Avsc` contracts according to the ADR. Verify schema evolution and versioning remain correct for specialised schemas.
6. **Integrate all transports and provisioning.** Update Kafka, Pub/Sub and SNS/SQS producers, deserializers, subscribers and topic/queue/subscription provisioning to propagate and resolve the same identity. Test round trips and isolation for each platform and document any Avro schema-resolution limitation inherited from TASK-275.
7. **Integrate streams and topology test APIs.** Preserve the key through `PrefabStreams.from/to`, join, aggregate, processors, stores and tracing metadata; make generic source/target type tokens explicit where erasure prevents inference. Add backend-neutral topology contract tests and Kafka topology tests for parameterisation-specific routing, state restoration and unchanged non-generic topology identity.
8. **Integrate versioning/evolution, finish docs and examples.** Extend TASK-274/TASK-301 validators, baselines, migrations and compatibility checks to use the chosen generic identity without changing non-generic contracts. Update annotation reference, generated artefact/event/streams guides and one end-to-end example; then run focused module tests followed by the full Maven suite.

### Verification gates
- Gate 1: ADR resolves the identity/API/compatibility decisions before implementation starts.
- Gate 2: one concrete generic event can be published, JSON-deserialized and routed exclusively to its concrete handler; a second specialisation on the same topic is not cross-delivered.
- Gate 2a: compile the agreed repeatable declaration example; assert separate topic registrations/provisioning and that publishing either parameterisation never targets the other's topic or handler. Verify multi-topic fan-out is scoped to one binding.
- Gate 3: Avro specialisations have deterministic distinct schema names and reader registrations, with collision and evolution tests.
- Gate 4: transport and stream contract tests pass for each supported path; generated artefacts for existing non-generic events and their topics/wire formats remain unchanged.
- Gate 5: unsupported forms fail at compile time with clear diagnostics, documentation/examples match supported behavior, and the complete Maven test suite passes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

- Implemented the agreed bound-publisher API in `core/domain/EventPublisher`, with concrete reflective tokens in `EventType` and infrastructure-only `TypedEvent` envelopes. Non-generic bindings publish their original payloads unchanged; generic handles retain their concrete token.
- Deprecated legacy `PublishesEvents`, `DomainEventPublisher` and dispatcher publication methods (since 0.12.2), preserving legacy helper behavior. Handles require an active infrastructure publisher and resolve it at call time, allowing static aggregate fields and publisher replacement between tests.
- `CapturingDomainEventPublisher` unwraps typed publications so existing payload assertions remain useful. Spring dispatcher selection inspects payload classes; unclaimed generic envelopes fail explicitly rather than leaking onto the Spring event bus.
- Added runtime tests for lifecycle/rebinding, raw and wildcard rejection, token identity, payload capture and registry isolation; compiler tests verify generic payload mismatches, deprecation warnings and aggregate generation with a static bound publisher.
- Added repeatable annotation and concrete-key registry foundations. These are not yet wired through all processor discovery, serializers, transport consumers, Avro converters, streams or versioning. TASK-303 remains In Progress; only AC #1 and #12 are complete.
- Extended the foundation with repeatable declaration/dependency discovery, early binding diagnostics, resolved generic member fields, separate typed registrars, fixture builders and AsyncAPI component identities. Dependent/self-referential bounds are substituted safely; unsupported handler bindings fail before generation.
- Kafka JSON now stamps the concrete identity and binding version, serializes only the payload, resolves Jackson's full concrete target type from a topic-local registry allowlist, and checks minimum/event/component major versions. Generated listeners select their binding's topics/platform and guard by concrete identity before invoking a handler. Generic Avro currently fails with an explicit compiler diagnostic; generic event/component migrations are explicitly rejected when required.
- Added compiler tests for binding discovery, invalid argument forms, duplicate bindings and bounds; runtime JSON tests for shared topics, spoofed/missing identity, payload wire shape, component metadata and version rejection; tests invoke the actual compiled generated listeners to verify shared-topic isolation. Documentation describes this partial integration and its remaining limits.
- Verification of the Kafka JSON slice: the full `mvn -q -pl kafka -am test` reactor passed. Final focused checks including `GenericKafkaBindingTest`, `GenericEventBindingTest`, `GenericJsonEventTest`, `EventSchemaDocumentationWriterTest`, `EventVersionsTest` and `EventMigratorTest` passed after fixing a test fixture's `PUBSUB`/`PUB_SUB` enum spelling. The subsequent full `mvn -q --fail-at-end test` attempt reached the Pub/Sub example but stalled pulling `gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators`; its 120-second pull watchdog aborted and retried. Stopped the stalled owned Maven process. This attempt is not a full green reactor; its earlier Kafka fixture failure was corrected and independently rerun. Logs are retained in the session's `files/task-303-kafka-reactor.log`, `task-303-bindings-final.log` and `task-303-reactor-current.log`.
- Remaining work: concrete Avro schemas/converters/reader registration; Pub/Sub and SNS/SQS runtime/generation plus provisioning; stream and topology APIs; per-binding evolution baselines and event/component migrations. Leave TASK-303 In Progress and do not infer completion of AC #2-11 from this Kafka-only slice.
- Subsequent transport slice implements Pub/Sub and SNS/SQS JSON runtime/generation. Tests cover original payload bytes, concrete deserialization, shared-topic skips and acknowledgement/deletion, message identity/type/version attributes, exact-platform dispatch through Spring, generated token subscriptions on literal/shared/property-expression topics, and deterministic broker-safe names. The full `mvn -q -pl kafka,pubsub,sns-sqs -am test` module reactor passed; final focused checks additionally cover actual SQS consumption and the new platform-dispatch scenarios.
- Still unfinished: generic Avro; declarative provisioning (including repeated-binding discovery in Terraform); stream/topology APIs; per-binding evolution baselines and event/component migrations. Existing runtime Pub/Sub and SNS/SQS subscribe/publish paths create their topics/subscriptions/queues, but this does not complete declarative provisioning or the broader task. Keep TASK-303 In Progress.
- Committed the concrete JSON transport slice as `49210fdf`. AC #2 and #4 are now checked based on the concrete JSON runtime and generated-handler coverage; provisioning and broader compatibility acceptance criteria remain unchecked.
- Attempted the complete `mvn -q --fail-at-end test` reactor. `examples/avro` failed to load its application context because the Kafka Testcontainer timed out waiting for `Transitioning from RECOVERY to RUNNING`. The subsequent Kafka example stalled pulling `confluentinc/cp-schema-registry:8.4.0` with one image layer pending; stopped only this owned Maven run. Full-suite verification is therefore not green. The affected module reactor and final focused checks passed; full output is retained in session artifact `task-303-transports-full.log`.
- Focused publication/compiler/test-infrastructure checks pass. The first full `mvn test` run stopped on an `EventConsumerWhereStepTest.customAssertClassPropagatesAssertionFailures` timeout; the same test passed on isolated retry. A second `mvn --fail-at-end test` run reached `SaleIntegrationTest` but could not fetch `confluentinc/cp-schema-registry:8.4.0` (connection reset / HTTP 416); the Kafka example then stalled pulling the same image and hit the 120-second pull watchdog. Stopped the repeated image pulls. The complete reactor is not green and later modules were not all verified; do not treat this as full TASK-303 completion.
