---
id: TASK-260
title: Support global state stores in Prefab Streams
status: Done
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
- [x] #1 Streams DSL exposes a clear way to declare and use a global state store.
- [x] #2 Kafka backend materializes the global store once per topology and makes it available to the relevant processors.
- [x] #3 Multiple stream branches or processors that share the same store definition observe consistent read/write behaviour.
- [x] #4 Invalid or missing store bindings fail fast with actionable errors.
- [x] #5 Tests cover shared access, lifecycle behaviour, and failure scenarios.
- [x] #6 Developer documentation and examples explain when to use a global state store versus a local per-processor store.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Clarify the contract as a Kafka global store: read-only, fed from a source topic, fully replicated to every instance (a partitioned ddStateStore store is NOT global).
2. Add PrefabStreams.globalStore(Class) returning a read-only GlobalStore, reusable by any processor or branch.
3. Reuse the existing `StateStore` / `process(...)` direction from TASK-216 so the shared store stays backend-agnostic.
4. Materialise via StreamsBuilder.addGlobalStore(...) once per type, with a terminal nd() to obtain a StreamDefinition after process(...).
5. Validate sharing, lifecycle, and fail-fast behaviour with focused tests before widening docs/examples.
6. Store identity is the value type (global-{type}).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Correction (2026-10-28): the first implementation (`sharedStore(...)`) used `StreamsBuilder.addStateStore(...)`, which creates a partitioned, per-task store, not a global one, and `process(...)` offered no way to obtain a `StreamDefinition`. The spec was wrong and has been replaced.

Implementation:
- Removed `sharedStore(...)`. Added `PrefabStreams.globalStore(Class<V>)` returning `GlobalStore<K, V>` (read-only; new `ReadableStore` supertype shared with `Store`).
- `KafkaPrefabStreams` registers the store with `addGlobalStore(...)` (logging disabled, source = topic of the value type) and a `GlobalStoreUpdateProcessor` that puts records and deletes on `null` values. Repeated declarations return the same instance.
- `process(processor, ReadableStore...)` accepts local and global stores; `StreamProcessor.globalStores()` added. Global stores are not connected to the processor node (Kafka exposes them read-only to every processor); undeclared global stores fail fast.
- Added `PrefabStream.end()` as a sink-less terminator returning the `StreamDefinition`; `to(...)` delegates to it.
- Tests in `KafkaPrefabStreamsTopologyTest`: global store present in the topology description, reads from source topic, tombstone removal, repeated declaration, undeclared binding failure, `end()`.
- Docs: `backlog/docs/feature-guides.md` (Global state stores, Ending a topology without a sink).

Follow-up: stores can be named explicitly (`globalStore(name, type)`), and `globalStore(name, sourceType, storeType, mapper)` decouples the stored type from the source topic's type, since Kafka allows one global store per source topic (duplicate source topics, duplicate names with other types fail fast). Differing store types use a registry-independent JSON serde.

Follow-up 2: `globalStoreFrom(sourceType).updatedBy(GlobalStoreUpdater).as([name,] storeType)` for stores computed from source records (a processor can never write a global store; the updater is the single consume-only writer, passed a writable `Store` per record, no forwarding/context). Returns only the read-only `GlobalStore`; backed by `addGlobalStore` with a writer processor. Verified in Kafka 4.2.1 that `addGlobalStore` sets reprocessOnRestore=true, so restores replay through the updater; side effects therefore repeat on restore and run on every instance (documented).

Verification: `mvn -pl streams,test,examples/streams -am test` passes.<!-- SECTION:NOTES:END -->

- Follow-up: removed the process(processor, stores...) overload. Processors declare the global stores they read via StreamProcessor.globalStores(); KafkaPrefabStreamProcessorAdapter.init binds them.
