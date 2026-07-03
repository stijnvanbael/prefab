---
id: TASK-260
title: Support global state stores in Prefab Streams
status: In Progress
assignee: []
created_date: '2026-07-02 06:32'
updated_date: '2026-07-03 05:31'
labels:
  - feature
  - streams
  - state-store
  - kafka
milestone: m-4
dependencies:
  - TASK-216
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - >-
    backlog/tasks/task-216 -
    Implement-process...-with-pluggable-StateStore-support.md
priority: high
ordinal: 167000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add support for globally shared state stores in Prefab Streams so stream processors can resolve the same store across a topology without backend-specific wiring. The task should align with the existing `StateStore` / `process(...)` direction and preserve backend-agnostic DSL semantics for users.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL exposes a clear way to declare and use a global state store.
- [ ] #2 Kafka backend materializes the global store once per topology and makes it available to the relevant processors.
- [ ] #3 Multiple stream branches or processors that share the same store definition observe consistent read/write behaviour.
- [ ] #4 Invalid or missing store bindings fail fast with actionable errors.
- [ ] #5 Tests cover shared access, lifecycle behaviour, and failure scenarios.
- [ ] #6 Developer documentation and examples explain when to use a global state store versus a local per-processor store.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Clarify the contract as a topology-scoped shared store, not Kafka GlobalKTable semantics.
2. Add an explicit DSL-level store declaration that can be reused by more than one processor or branch.
3. Reuse the existing `StateStore` / `process(...)` direction from TASK-216 so the shared store stays backend-agnostic.
4. Add Kafka-backed materialization that registers the store once per topology and binds all consumers to the same logical instance.
5. Validate sharing, lifecycle, and fail-fast behaviour with focused tests before widening docs/examples.
6. Review whether store identity should be name + type to avoid accidental sharing of unrelated stores.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Usage example to keep in scope:
```java
var customerProfiles = streams.sharedStore("customer-profiles", CustomerId.class, CustomerProfile.class);

streams.from(OrderPlaced.class).process(enrichmentProcessor, customerProfiles);
streams.from(CustomerUpdated.class).process(profileUpdater, customerProfiles);
```
This shows the intended behaviour: one topology-scoped store reused by multiple processors, with shared reads and writes.

Open questions to resolve during implementation:
- What exact DSL method name best communicates shared vs local store scope?
- Should the shared store be declared explicitly by name, or inferred from the `StateStore` definition?
- How should the Kafka backend guard against duplicate incompatible store declarations?
- Do non-Kafka backends need the same sharing contract now, or only the backend-agnostic abstraction?

Implemented topology-scoped shared stores in Streams DSL: added `PrefabStreams.sharedStore(...)` declarations and Kafka backend registry that materializes each shared store once per topology and reuses the same store adapter instance for compatible redeclarations.

Added fail-fast validation for incompatible shared store redeclarations (same name, different key/value contract) with actionable `IllegalArgumentException` messages.

Added explicit `process(processor, Store... stores)` binding flow via `StoreBindingStreamProcessor` so non-`StatefulStreamProcessor` processors can bind and initialize shared stores safely; added conflict validation for duplicate store names with different instances.

Extended topology tests with shared-store scenarios: cross-branch shared read/write behavior, declaration reuse identity, incompatible declaration failure, and conflicting process binding failure.

Updated `backlog/docs/feature-guides.md` Streams section with shared-store usage guidance, when to choose shared vs local stores, and failure semantics.

Verification: `mvn -pl streams -am test -Dtest=KafkaPrefabStreamsTopologyTest -Dsurefire.failIfNoSpecifiedTests=false` (pass), `mvn -pl test -am test -DskipTests` (pass compile for dependent modules).
<!-- SECTION:NOTES:END -->
