---
id: M-011
title: Define PersistenceProvider and MessagingProvider SPIs for extensibility
status: To Do
assignee: []
created_date: '2026-05-08 16:38'
labels: []
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Define clean `PrefabPersistenceProvider` and `PrefabMessagingProvider` SPIs in `prefab-core`. All current backends (PostgreSQL, MongoDB, Kafka, Pub/Sub, SNS/SQS) become implementations of these SPIs. This allows community contributions (e.g. MySQL, RabbitMQ, Pulsar) without modifying core processor code.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 PrefabPersistenceProvider SPI is defined in prefab-core
- [ ] #2 PrefabMessagingProvider SPI is defined in prefab-core
- [ ] #3 PostgreSQL, MongoDB, Kafka, Pub/Sub, and SNS/SQS modules implement these SPIs
- [ ] #4 A new persistence or messaging backend can be added without modifying prefab-core or the annotation processor
- [ ] #5 Developer Guide documents the SPIs with a tutorial for writing a custom provider
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
