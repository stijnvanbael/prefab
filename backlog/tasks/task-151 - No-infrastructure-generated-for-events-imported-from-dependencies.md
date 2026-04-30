---
id: TASK-151
title: No infrastructure generated for events imported from dependencies
status: Done
assignee: []
created_date: '2026-04-30 07:23'
updated_date: '2026-04-30 13:52'
labels: []
dependencies: []
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When an @Event-annotated type is defined in a dependency JAR rather than in the current compilation sources, no messaging infrastructure (producer/publisher/consumer/subscriber) is generated for it.
Root cause: All three platform plugins (Kafka, Pub/Sub, SNS/SQS) rely on RoundEnvironment#getElementsAnnotatedWith(Event.class) and/or PrefabContext#eventElements() to discover events to publish. Both sources only return elements that are part of the current compilation round, i.e. source files being compiled. Classpath elements from dependency JARs are invisible to these queries.
Affected code:
- KafkaPlugin#writePublishers uses context.eventElements() which uses roundEnvironment.getElementsAnnotatedWith(Event.class) and getRootElements()
- PubSubPlugin#writePublishers uses roundEnvironment.getElementsAnnotatedWith(Event.class) directly
- SnsPlugin#writePublishers uses roundEnvironment.getElementsAnnotatedWith(Event.class) directly
Consumer/subscriber generation is driven by @EventHandler methods in the current source, so it may partially work, but the producer side silently produces nothing for imported events.
Fix: Extend event discovery to also scan the classpath for @Event-annotated types. This requires using processingEnvironment.getElementUtils() to look up known types, or scanning the annotation processor classpath via a different mechanism.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A Kafka producer is generated when the @Event type comes from a dependency JAR
- [ ] #2 A Pub/Sub publisher is generated when the @Event type comes from a dependency JAR
- [ ] #3 An SNS publisher is generated when the @Event type comes from a dependency JAR
- [ ] #4 PrefabContext#eventElements also returns @Event types from the classpath that are referenced by @EventHandler methods in current sources
- [ ] #5 No duplicate infrastructure is generated if the event type appears both in sources and on the classpath
<!-- AC:END -->
