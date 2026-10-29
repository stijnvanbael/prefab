---
id: decision-003
title: Event versioning with typed snapshot migrations
date: '2026-10-29'
status: accepted
related_tasks:
  - TASK-301
  - TASK-274
---

## Context

TASK-274 covers non-breaking evolution only. Breaking changes (split fields, unit changes, lookups) need arbitrary code.

## Decision

- Events carry a semantic version (`@Event(version)`, default `1.0.0`). Producers stamp `prefab-event-version` (and `prefab-component-versions` for `@Versioned` nested types) as Kafka headers, Pub/Sub attributes and SNS/SQS message attributes. A missing header means `1.0.0`.
- Migrations are typed Java code: `Migration<Old, New>` annotated with `@EventMigration(from, to)` (majors only) and registered as Spring beans. Snapshots are plain records of the historic shape; the runtime binds the payload (JSON or Avro, via a Jackson tree) to the snapshot, so user code never sees `JsonNode` or `GenericRecord`.
- Chains are per major. Nested component migrations run first, inner-first, wherever the component occurs.
- Routing: same major reads directly (unknown fields ignored, which gives forward compatibility); lower major runs the chain; higher major, missing chain step or below `minSupportedVersion` throws `UnsupportedEventVersionException`, which flows to the transport's existing DLT/redrive path.
- Compile-time gating: the annotation processor compares each event against a committed baseline contract (`prefab/event-baseline/<fqcn>.properties`) under the configured compatibility mode (`BACKWARD` default, `prefab.events.compatibility`). Major bumps need a migration chain; changed contracts need a version bump.

## Consequences

- Old snapshot and migration code can be deleted once retention expires old messages.
- Baselines must be committed and updated when releasing a version.
- Avro on Pub/Sub and SNS/SQS remains limited by TASK-275, but migrations still run from the version header.
