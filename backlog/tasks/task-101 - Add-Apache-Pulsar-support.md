---
id: TASK-101
title: Add Apache Pulsar support
status: To Do
assignee: []
created_date: '2026-03-31 07:33'
updated_date: '2026-04-24 06:57'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 124000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Apache Pulsar is a cloud-native, distributed messaging and streaming platform. Like the existing Kafka and SNS/SQS modules, Prefab should support Apache Pulsar as a first-class messaging backend so that users can publish and consume domain events over Pulsar topics with zero boilerplate.

The guiding principle is Prefab's philosophy of **start high, dive deep when you need to**: users should be able to switch to (or adopt) Pulsar by adding a single dependency and configuring a broker URL, with no changes to their domain model, `@Event` annotations, or `@EventHandler` methods.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Create a new `prefab-pulsar` Maven module (annotation-processor plugin + runtime support) following the same structure as `prefab-kafka` and `prefab-sns-sqs`
- [ ] #2 Implement `PulsarPlugin` (implements `PrefabPlugin`) that drives code generation for Pulsar producers and consumers, registered in `META-INF/services/be.appify.prefab.processor.PrefabPlugin`
- [ ] #3 Implement `PulsarProducerWriter` that generates a `{EventName}PulsarProducer` Spring component with an `@EventListener` method that publishes the event to the configured Pulsar topic using Spring for Apache Pulsar's `PulsarTemplate`
- [ ] #4 Implement `PulsarConsumerWriter` that generates a `{OwnerName}PulsarConsumer` Spring component with a `@PulsarListener`-annotated method that deserializes incoming messages and routes them to the matching `@EventHandler` method
- [ ] #5 Add runtime support in `prefab-core` (or a new `prefab-pulsar` runtime sub-module): `PulsarConfiguration` auto-configuration class wiring up serialization, schema, and error-handling beans; registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- [ ] #6 Support both JSON and Avro serialization for Pulsar messages, consistent with the serialization strategy used by the Kafka module (`DynamicSerializer`/`DynamicDeserializer`)
- [ ] #7 Support `@EventHandlerConfig` attributes (`concurrency`, `deadLetteringEnabled`, `deadLetterTopic`, `retryLimit`, backoff settings) for generated Pulsar consumers, mapping them to the appropriate Spring Pulsar listener container configuration
- [ ] #8 Support `@PartitioningKey` for produced messages so that events with the same key are routed to the same Pulsar partition/key
- [ ] #9 Add `prefab-pulsar` as a module in the root `pom.xml`
- [ ] #10 Add an `examples/pulsar` module (or extend an existing example) that demonstrates publishing and consuming domain events over Pulsar, including a `docker-compose.yml` for a local Pulsar standalone broker
- [ ] #11 Add integration tests for `PulsarPlugin` code generation (following the pattern in `KafkaPluginTest`) and runtime integration tests using Testcontainers (`apachepulsar/pulsar` image)
- [ ] #12 README / module documentation updated to describe `prefab-pulsar`, including required dependencies, configuration properties (`spring.pulsar.client.service-url`, topic naming), and a quick-start example
<!-- AC:END -->
