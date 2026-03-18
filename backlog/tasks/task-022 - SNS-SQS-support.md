---
id: TASK-022
title: SNS/SQS support
status: To Do
assignee:
  - '@agent'
created_date: '2025-10-10 13:38'
updated_date: '2026-03-19 06:52'
labels: []
dependencies: []
ordinal: 113.90566825866699
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Functional Analysis

AWS SNS/SQS support adds a new messaging platform option to Prefab, complementing the existing Kafka and Google Pub/Sub integrations. Users annotate their domain events with `@Event(topic = "...", platform = Platform.SNS_SQS)` and Prefab will automatically generate:

- **SNS Publisher** classes that listen for Spring domain events and publish them to AWS SNS topics.
- **SQS Subscriber** classes that consume messages from SQS queues and route them to methods annotated with `@EventHandler`.

The feature follows the same developer experience as Kafka and Pub/Sub:

```java
// 1. Annotate the event with the SNS topic
@Event(topic = "user-events", platform = Platform.SNS_SQS)
public sealed interface UserEvent permits UserEvent.Created {
    record Created(Reference<User> user, String name) implements UserEvent {}
}

// 2. Publish events from aggregate roots (unchanged)
publish(new UserEvent.Created(id, name));

// 3. Consume events using @EventHandler (unchanged)
@Component
public class UserExporter {
    @EventHandler
    public void onUserCreated(UserEvent.Created event) { ... }
}
```

Prefab generates:
- `UserEventSnsPublisher` that listens for `UserEvent` via `@EventListener` and publishes to the SNS topic.
- `UserExporterSqsSubscriber` that uses `@SqsListener` to consume from the corresponding SQS queue and routes to the `onUserCreated` handler.

### AWS SNS/SQS Topology

The SNS/SQS fan-out pattern is used:
- Each event type maps to one **SNS topic**.
- Each event-handler owner maps to one **SQS queue** that subscribes to the SNS topic, named `<app-name>-<owner>-on-<event>`.
- Dead-Letter Queues (DLQ) are created automatically and wired to failed SQS queues.
- SQS queues are subscribed to the SNS topic automatically at startup (if they do not already exist).

### Configuration

Users configure AWS credentials via standard Spring Cloud AWS properties:

```properties
spring.cloud.aws.region.static=eu-west-1
spring.cloud.aws.credentials.access-key=...
spring.cloud.aws.credentials.secret-key=...
# Optional: custom dead-letter queue name
prefab.dlt.name=my-app-dlt
# Optional: retry configuration
prefab.dlt.retries.limit=5
prefab.dlt.retries.minimum-backoff-ms=1000
prefab.dlt.retries.maximum-backoff-ms=30000
prefab.dlt.retries.backoff-multiplier=1.5
```

### Dead-Letter Queue (DLQ)

Failed messages are routed to a Dead-Letter Queue after exceeding the configured retry limit. The DLQ name defaults to `<spring.application.name>.dlt` and can be overridden via `prefab.dlt.name`. Custom DLQ configuration per event-handler can be set via `@EventHandlerConfig(deadLetterTopic = "...", deadLetteringEnabled = false)`.

### Serialization

Messages are serialized to JSON using Jackson. The event class name is included as a message attribute (`type`) to support polymorphic deserialization of sealed event interfaces.

## Technical Analysis

### Module Structure

A new Maven module `prefab-sns-sqs` is created following the same pattern as `prefab-kafka` and `prefab-pubsub`:

```
sns-sqs/
├── pom.xml
└── src/main/java/be/appify/prefab/processor/event/sns/
    ├── SnsPlugin.java          - PrefabPlugin implementation
    ├── SnsPublisherWriter.java - Generates SNS publisher classes
    └── SqsSubscriberWriter.java - Generates SQS subscriber classes
```

### Core Runtime Support

New classes in `prefab-core` under `be.appify.prefab.core.sns`:

| Class | Purpose |
|---|---|
| `SnsConfiguration` | Spring `@Configuration` for SNS/SQS beans; uses `@ConditionalOnClass(SnsTemplate.class)` |
| `SqsUtil` | Runtime utility for queue creation, subscription to SNS topics, JSON deserialization, and DLQ management |
| `SqsSubscriptionRequest<T>` | Value object encapsulating queue name, topic ARN, event type, consumer, retry template, and DLQ config |

Dependencies added to `prefab-core/pom.xml` (scope: `provided`):
```xml
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-sns</artifactId>
</dependency>
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-sqs</artifactId>
</dependency>
```

Dependencies in `prefab-sns-sqs/pom.xml`:
```xml
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-annotation-processor</artifactId>
</dependency>
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-sns</artifactId>
</dependency>
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-sqs</artifactId>
</dependency>
```

### Platform Enum Change

Add `SNS_SQS` to `Event.Platform` in `core/src/main/java/be/appify/prefab/core/annotations/Event.java`:

```java
SNS_SQS {
    @Override
    public String toString() {
        return "SNS/SQS";
    }
}
```

### SnsPlugin (annotation processor)

`SnsPlugin` follows the same structure as `KafkaPlugin` and `PubSubPlugin`:
- Constructor calls `setDerivedPlatform(Event.Platform.SNS_SQS)`
- `initContext()` creates `SnsPublisherWriter` and `SqsSubscriberWriter`
- `writeAdditionalFiles()` calls `writePublishers()` and `writeConsumers()`
- Registered as a `PrefabPlugin` via `@AutoService`

### SnsPublisherWriter

Generates a class `{EventName}SnsPublisher` in `infrastructure.sns`:

```java
@Component
public class UserEventSnsPublisher {
    private static final Logger log = LoggerFactory.getLogger(UserEventSnsPublisher.class);
    private final SnsTemplate snsTemplate;
    private final String topicArn;

    public UserEventSnsPublisher(SnsTemplate snsTemplate, SqsUtil sqsUtil) {
        this.snsTemplate = snsTemplate;
        this.topicArn = sqsUtil.ensureTopicExists("user-events");
    }

    @EventListener
    public void publish(UserEvent event) {
        log.debug("Publishing event {} on topic {}", event, topicArn);
        snsTemplate.sendNotification(topicArn, event, event.getClass().getName());
    }
}
```

Key decisions:
- Uses `SnsTemplate.sendNotification()` from Spring Cloud AWS.
- Topic ARN is resolved at startup via `SqsUtil.ensureTopicExists()`, which creates the topic if it does not exist.
- The event class name is passed as the subject (used for polymorphic type resolution on the consumer side).

### SqsSubscriberWriter

Generates a class `{OwnerName}SqsSubscriber` in `infrastructure.sns`:

```java
@Component
public class UserExporterSqsSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterSqsSubscriber.class);
    private final UserExporter userExporter;

    public UserExporterSqsSubscriber(UserExporter userExporter, SqsUtil sqsUtil) {
        sqsUtil.subscribe(new SqsSubscriptionRequest<>(
            "user-events",
            "${spring.application.name}.user-exporter-on-user-event",
            UserEvent.class,
            this::onUserEvent
        ));
        this.userExporter = userExporter;
    }

    private void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userExporter.onUserCreated(e);
        }
    }
}
```

Alternatively, `@SqsListener` can be used for simpler cases; however, using `SqsUtil` (similar to `PubSubUtil`) allows programmatic queue and subscription management at startup.

### SqsUtil (core runtime)

`SqsUtil` is a Spring `@Component` annotated with `@ConditionalOnClass(SnsTemplate.class)`:

- `ensureTopicExists(String topicName)`: Creates the SNS topic if it does not exist and returns the topic ARN.
- `subscribe(SqsSubscriptionRequest<T> request)`: Creates the SQS queue (if missing), subscribes the queue to the SNS topic, configures DLQ redrive policy, and registers a `SqsAsyncClient` message listener.
- JSON deserialization using Jackson, with polymorphic type detection via the SNS message `Subject` attribute.
- DLQ management mirroring `PubSubUtil`: creates a `<queueName>-dlt` queue and wires the redrive policy.

### Retry / DLQ Handling

AWS SQS natively supports redrive policies. `SqsUtil` sets:
- `maxReceiveCount` (from `prefab.dlt.retries.limit:5`)
- `deadLetterTargetArn` pointing to the DLQ

Spring's `RetryTemplate` is used for application-level retries before a message is nacked (returned to the queue for SQS-level retry).

### Example Application

A new example module `examples/sns-sqs` is created following the same structure as `examples/kafka` and `examples/pubsub`:
```
examples/sns-sqs/
├── pom.xml (dependency: prefab-sns-sqs, spring-cloud-aws-starter-sns, spring-cloud-aws-starter-sqs)
└── src/main/java/be/appify/prefab/example/sns/
    ├── user/
    │   ├── User.java
    │   ├── UserEvent.java (@Event with Platform.SNS_SQS)
    │   └── UserExporter.java (@EventHandler methods)
    └── SnsSqsApplication.java
```

### Test Coverage

Following the same pattern as `kafka` and `pubsub` test modules:
- `SnsPublisherWriterTest` - verifies generated SNS publisher code via `compile-testing`
- `SqsSubscriberWriterTest` - verifies generated SQS subscriber code
- Test resource fixtures under `src/test/resources/expected/sns/single`, `multiple`, etc.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add SNS_SQS value to Event.Platform enum in core module
- [ ] #2 Add spring-cloud-aws-starter-sns and spring-cloud-aws-starter-sqs as provided dependencies in prefab-core
- [ ] #3 Create prefab-sns-sqs Maven module with pom.xml depending on prefab-annotation-processor, spring-cloud-aws-starter-sns, and spring-cloud-aws-starter-sqs
- [ ] #4 Implement SqsUtil in prefab-core for programmatic SNS topic creation, SQS queue creation, SNS-to-SQS subscription wiring, and DLQ redrive policy management
- [ ] #5 Implement SnsConfiguration in prefab-core as a @ConditionalOnClass(SnsTemplate.class) Spring configuration that component-scans SqsUtil
- [ ] #6 Implement SnsPlugin in prefab-sns-sqs as a PrefabPlugin registered via @AutoService that sets derived platform to SNS_SQS
- [ ] #7 Implement SnsPublisherWriter that generates {EventName}SnsPublisher classes using SnsTemplate to publish events to SNS topics
- [ ] #8 Implement SqsSubscriberWriter that generates {OwnerName}SqsSubscriber classes that consume from SQS queues and route to @EventHandler methods
- [ ] #9 Support property placeholder topics (e.g. ${topics.user.name}) in both publisher and subscriber
- [ ] #10 Support @EventHandlerConfig for custom DLQ name, deadLetteringEnabled=false, custom retries (retryLimit, minimumBackoffMs, maximumBackoffMs, backoffMultiplier)
- [ ] #11 Support multiple event types per subscriber (multi-topic scenario)
- [ ] #12 Register prefab-sns-sqs as a module in the parent pom.xml
- [ ] #13 Create examples/sns-sqs example module demonstrating SNS/SQS event publishing and consuming
- [ ] #14 Add unit tests for SnsPublisherWriter and SqsSubscriberWriter using compile-testing, with expected generated code fixtures
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add SNS_SQS to Event.Platform enum in core/src/main/java/be/appify/prefab/core/annotations/Event.java
2. Add spring-cloud-aws dependencies (provided scope) to prefab-core pom.xml
3. Implement SqsSubscriptionRequest<T> value object in be.appify.prefab.core.sns (mirrors PubSub SubscriptionRequest)
4. Implement SqsUtil @Component in be.appify.prefab.core.sns: ensureTopicExists(), ensureQueueExists(), subscribe(), DLQ wiring via redrive policy
5. Implement SnsConfiguration @Configuration in be.appify.prefab.core.sns with @ConditionalOnClass(SnsTemplate.class) and @ComponentScan
6. Create prefab-sns-sqs/pom.xml with parent, dependencies on prefab-annotation-processor + spring-cloud-aws starters
7. Implement SnsPlugin in be.appify.prefab.processor.event.sns, registered via @AutoService
8. Implement SnsPublisherWriter generating {EventName}SnsPublisher in infrastructure.sns package
9. Implement SqsSubscriberWriter generating {OwnerName}SqsSubscriber in infrastructure.sns package
10. Add prefab-sns-sqs module to parent pom.xml <modules> section
11. Create examples/sns-sqs module with User, UserEvent (@Event platform=SNS_SQS), UserExporter example
12. Add SnsPublisherWriterTest and SqsSubscriberWriterTest with compile-testing, plus expected fixture files
13. Run mvn test on kafka and pubsub modules to verify no regressions
14. Run mvn test on sns-sqs module to verify all tests pass
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Functional and Technical Analysis for AWS SNS/SQS Support

This analysis defines the scope, design, and acceptance criteria for adding AWS SNS/SQS as a first-class messaging platform in Prefab, alongside the existing Kafka and Google Pub/Sub integrations.

### Summary

- **New platform value**: `Event.Platform.SNS_SQS` added to the `@Event` annotation enum.
- **New Maven module**: `prefab-sns-sqs` with annotation-processor plugin for code generation.
- **Generated code**: `{EventName}SnsPublisher` (SNS publish) and `{OwnerName}SqsSubscriber` (SQS consume).
- **Core runtime**: `SqsUtil` for topic/queue auto-creation, SNS-SQS subscription, DLQ wiring.
- **Full parity** with Kafka/PubSub: property-placeholder topics, `@EventHandlerConfig`, multi-topic consumers, DLT/retries.
- **Example module**: `examples/sns-sqs` demonstrating end-to-end usage.
- **Tests**: `compile-testing`-based unit tests with expected generated code fixtures.

No breaking changes are introduced. Existing Kafka and Pub/Sub integrations are unaffected.
<!-- SECTION:NOTES:END -->
