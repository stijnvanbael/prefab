---
id: decision-004
title: Concrete generic event bindings
date: '2026-10-30'
status: accepted
related_tasks:
  - TASK-303
---

## Context

Different parameterisations of one event record have the same Java runtime class.
Inferring arguments from payload values is unreliable, especially for nulls, empty
collections and inheritance.

## Decision

- Make `@Event` repeatable. Each declaration binds its positional `typeArguments`
  class literals to its own explicit topics, format and fan-out strategy.
- Bind publication identity once with
  `EventPublisher.bind(new EventType<Changed<Customer>>() {})`. For non-generic
  events, use `EventPublisher.bind(CustomerCreated.class)`. Both handles expose
  `publish(event)` and resolve the current infrastructure publisher on each call,
  never during static initialization. A missing publisher fails explicitly.
  The backend-neutral token retains the reflective type. A publication envelope
  is infrastructure metadata, never a payload wrapper on the wire.
- Deprecate legacy direct `publish` and `publishEvent` application entry points,
  retaining existing behavior during migration. The infrastructure override hook
  remains available. Deprecation is not a compile-time prohibition; removal can
  happen in a future breaking release.
- Canonical identity is the fully qualified reflective type name with comma
  whitespace removed. Resolve incoming identities against registered bindings,
  never by loading arbitrary class names from a message.
- Carry generic identity in `prefab-event-type` transport metadata. Leave existing
  non-generic payloads, headers and topic names unchanged.
- Generic declarations bind concrete non-generic classes. Raw arguments, wildcards,
  unresolved variables, argument-count errors and bound violations are rejected.
  Class literals cannot express a nested generic argument such as `List<Customer>`.
- Avro specialisations must use the raw schema name plus a SHA-256 digest of the
  canonical identity, with generation-time collision validation. Existing
  non-generic schema names are unchanged.
- Version/evolution contracts and migration targets must be tracked per concrete
  binding; specialisations must not share reader schemas or migration chains.
  The writer-schema limitation for Avro on Pub/Sub and SNS/SQS remains TASK-275.

## Consequences

Generic binding discovery must work without handlers and across dependency modules.
Dispatch, provisioning, serialization, versioning and streams must preserve the same
identity. A class-only publication of a generic event fails explicitly rather than
guessing a binding. Multi-topic fan-out belongs to one binding, never the erased class.
