---
id: TASK-366
title: Opt-in Micrometer observations for generated operations and event consumers
status: To Do
assignee: []
created_date: '2026-11-23 12:00'
labels:
  - observability
  - feature
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generated services, repositories and event consumers only emit `log.debug` lines. Adopters who want latency,
throughput and error metrics or traces per aggregate operation must either add Spring AOP around generated beans
or take over the generated classes. Found by the TASK-324 extension-point spike (decision-009, scenario 11).

Add an opt-in Micrometer `Observation` around each generated service operation and each generated event consumer
invocation, with low-cardinality tags (aggregate, operation, outcome; event type and topic for consumers). Reuse
Spring Boot's `ObservationRegistry`; do not add a dependency for applications that do not enable it.

Out of scope: stream-processing tracing (TASK-302), custom metric names per operation, high-cardinality tags such
as aggregate ids.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 With the option off (default) generated sources and runtime behaviour are unchanged; a test proves byte-identical output.
- [ ] #2 With the option on, every generated service operation and event-consumer invocation is wrapped in an observation with documented name and low-cardinality tags, recording errors.
- [ ] #3 Applications supply `ObservationHandler`s / `ObservationFilter`s as ordinary Spring beans; no Prefab-specific SPI is added.
- [ ] #4 Works for Kafka, Pub/Sub and SNS/SQS consumers and for PostgreSQL and MongoDB backed services; tests cover success and failure tags.
- [ ] #5 The option and observation names are documented in configuration.md and the extension point guide.
<!-- AC:END -->
