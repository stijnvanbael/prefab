---
id: TASK-301
title: Event versioning with typed migrations and compatibility modes
status: Done
assignee: []
created_date: '2026-10-29 10:15'
labels:
  - feature
  - events
  - avro
  - annotation-processor
  - kafka
  - pubsub
  - sns-sqs
dependencies:
  - TASK-274
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-274 handles non-breaking evolution declaratively (previous names, defaults, Avro reader-schema resolution). It cannot express breaking changes such as split/merged fields, changed units or lookups against a bean. Introduce explicit semantic versions on events and migrations written as arbitrary, typed Java code.

### Versioning
- `@Event(version = "2.1.0")`, default `1.0.0`. Producers stamp the full version in a `prefab-event-version` header (Kafka, Pub/Sub, SNS/SQS). Missing header = `1.0.0`.
- PATCH: no contract change. MINOR: backward-compatible change, no migration. MAJOR: breaking change, migration required.

### Migrations (typed snapshots)
```java
@EventMigration(from = "1", to = "2")
class UserCreatedMigration implements Migration<UserCreatedV1, UserCreated> {
    public UserCreated migrate(UserCreatedV1 old) { ... }
}
```
- Snapshots are plain, hand-written records/classes of the historic shape. Optionally generated from a separate AVSC file (`@EventSnapshot(avsc = "...")`) for Avro events.
- Migrations are Spring beans (DI allowed), chained per major (1 -> 2 -> 3). Snapshots and migrations can be deleted once topic retention has expired all messages of that major; the chain then rejects those versions explicitly.
- Runtime binds the payload to the snapshot via the existing JSON/Avro paths, so user code never touches ObjectNode or GenericRecord.
- Event type renames use `@EventMigration(fromType = "OldName")` / TASK-274 previous names for type resolution.

### Nested component migrations
A migration may target a nested component type instead of the top-level event: `Migration<AddressV1, Address>` is applied wherever `Address` occurs in any event, at any depth (including collections, optionals, maps), so event-level migrations need not navigate nested structure. Versioning of a nested type is tracked on that type (`@Versioned`/version attribute) and its migrations run before the enclosing event migration.

### Compatibility modes
Configurable per event (and default via configuration): `BACKWARD`, `FORWARD`, `FULL`, `NONE`, mirroring Confluent semantics.
- BACKWARD: new consumers read old data. FORWARD: old consumers read new data (unknown newer-minor fields ignored, new fields must be optional/defaulted). FULL: both.
- Compile-time gating against a committed baseline of the previous contract: a change violating the mode that is not a MAJOR bump is an error; a MAJOR bump without migration is an error; versions may not go backwards.

### Runtime rules
Header major equal to consumer major: read directly. Lower: run migration chain. Higher (producer ahead): DLT with a clear error.

Out of scope: Avro writer-schema resolution on Pub/Sub and SNS/SQS (TASK-275); migrations still run there using the version header.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `@Event(version)` accepts a semantic version; producers add the `prefab-event-version` header on Kafka, Pub/Sub and SNS/SQS; a missing header is read as 1.0.0 and contracts without a version behave exactly as today.
- [x] #2 `@EventMigration` on a `Migration<Old, New>` Spring bean migrates messages of an older major to the current one for JSON and Avro events, chaining multiple majors, with Old being a hand-written snapshot type.
- [x] #3 A snapshot for Avro events can be generated from a separate AVSC file; hand-written snapshots work for both formats.
- [x] #4 A migration declared on a nested component type is applied at any nesting depth (including lists, optionals and maps) without the event-level migration traversing the structure, and runs before the enclosing event migration.
- [x] #5 Removing old snapshots/migrations after retention is supported: versions with no remaining chain are rejected explicitly (DLT) rather than mis-deserialized, and the processor allows declaring a minimum supported version.
- [x] #6 Compatibility mode (BACKWARD, FORWARD, FULL, NONE) is configurable per event with a global default; the processor compares against a committed baseline contract and fails the build for changes violating the mode, MINOR bumps containing breaking changes, MAJOR bumps without a migration, non-contiguous chains, or decreasing versions.
- [x] #7 Forward compatibility works at runtime: a consumer reads messages with a higher minor/patch of the same major, ignoring unknown fields; a higher major is routed to the DLT with a descriptive error.
- [x] #8 Migrations can be unit-tested without a broker.
- [x] #9 Regression tests cover JSON and Avro, code-first and AVSC-generated events, multi-step chains, nested migrations, and each compatibility mode.
- [x] #10 backlog/docs/ documents versioning, migrations, snapshots, nested migrations, compatibility modes and the Pub/Sub / SNS/SQS Avro limitation, and an ADR in backlog/decisions/ records the typed-snapshot decision.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. ADR: typed snapshots, semver semantics, baseline contract format (decide where baseline is stored and how it is updated).
2. Core: version attribute, header stamping/reading, `Migration`/`EventMigration` API, `EventMigrator` (chain resolution, version routing).
3. JSON path: bind to snapshot, migrate, hook into DynamicDeserializer and Pub/Sub/SQS consumers.
4. Avro path: GenericRecord -> snapshot via generated converters (builds on TASK-274 reader-schema resolution); AVSC snapshot generation.
5. Nested migrations: component-level registry and traversal on the migrated tree/typed result.
6. Processor: discovery, generated migration registry, validation, baseline comparison and compatibility modes.
7. Tests, docs, example.
<!-- SECTION:PLAN:END -->


## Implementation Notes

Implemented in core (versioning package, Kafka/Pub/Sub/SNS-SQS transports), annotation-processor (EventVersioningValidator with baseline contracts) and avro-processor (@EventSnapshot). See decision-003 and docs/annotation-reference.md. Full build: all unit tests pass; Testcontainers-based example integration tests could not start Kafka containers locally (environmental timeouts).
