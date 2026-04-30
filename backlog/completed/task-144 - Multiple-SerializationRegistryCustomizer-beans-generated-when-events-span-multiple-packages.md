---
id: TASK-144
title: >-
  Multiple SerializationRegistryCustomizer beans generated when events span
  multiple packages
status: Done
assignee:
  - '@copilot'
created_date: '2026-04-27 14:10'
updated_date: '2026-04-30 06:04'
labels: []
dependencies: []
ordinal: 11000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When events are defined in multiple packages, the annotation processor generates a SerializationRegistryConfiguration per package. This results in multiple SerializationRegistryCustomizer beans conflicting with one another at runtime.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Only one SerializationRegistryConfiguration is generated regardless of how many packages events are spread across
- [x] #2 All topics from all packages are registered in the single customizer
- [x] #3 Existing single-package behaviour is unaffected
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Make the @Bean method name in SerializationRegistryConfiguration unique per package by deriving it from the common root package name
2. Write a test: compile two @Event-annotated classes in different sub-packages and assert a single SerializationRegistryConfiguration is generated with a unique bean name
3. Verify existing tests still pass
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fixed duplicate SerializationRegistryCustomizer bean name conflict when events span multiple packages.

**Root cause:** When two modules each contain @Event-annotated classes, each module's annotation processor generates a SerializationRegistryConfiguration with a @Bean method named serializationRegistryCustomizer. Spring Boot (bean override disabled by default) would throw a conflict or silently drop one, causing some topics to go unregistered.

**Fix:** In SerializationRegistryConfigurationWriter.beanMethod(), the @Bean method name is now derived from the common root package of all events (e.g. event.serialization.multipackage -> eventSerializationMultipackageSerializationRegistryCustomizer). Each module produces a uniquely named bean, all collected by SerializationRegistry via List<SerializationRegistryCustomizer>.

**Files changed:**
- annotation-processor/.../event/SerializationRegistryConfigurationWriter.java: compute rootPackage before TypeSpec construction; pass it to beanMethod(); derive unique bean method name via toCamelCase(rootPackage) + SerializationRegistryCustomizer
- annotation-processor/.../event/SerializationPluginTest.java: three tests covering single-package and multi-package event scenarios
- test resources: event/serialization/multipackage/source/order/OrderCreated.java and payment/PaymentProcessed.java

All annotation-processor tests pass.
<!-- SECTION:NOTES:END -->
