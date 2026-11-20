---
id: TASK-337.02
title: Showcase capability mechanism and technology coverage matrix
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
labels:
  - examples
  - testing
dependencies:
  - TASK-337.01
parent_task_id: TASK-337
priority: high
type: task
ordinal: 173000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Make technology gaps explicit in the showcase test runs and docs.

- A `Capability` enum in the showcase test sources (e.g. FULL_TEXT_SEARCH, RANGE_FILTER, POLYMORPHIC_AGGREGATES, BINARY_DOWNLOAD, ORDERED_DELIVERY, STREAMS, AVRO, SECURITY, OPENAPI, ASYNCAPI; extend as features land).
- `@RequiresCapability(...)` on tests, evaluated by a JUnit 5 `ExecutionCondition` that reads `showcase-capabilities.yml` from the variant's test resources. Each unsupported entry carries a reason and a backlog task ID; the skip message reads like `showcase-pubsub: STREAMS not supported — no Pub/Sub backend for Prefab Streams (TASK-xxx)`.
- Capabilities whose code lives in optional source folders (streams, avro) cannot have their tests compiled on unsupported variants; a `CapabilityGapsTest` emits one dynamic test per unsupported capability that is reported as skipped with its reason, so gaps show up in every build.
- `examples/README.md` contains a coverage matrix (variant × capability, with links to gap tasks). A test fails when the matrix and the capability files disagree.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Tests annotated with @RequiresCapability are skipped with variant, capability, reason and task ID when the variant lacks the capability
- [ ] #2 Unsupported capabilities without compiled tests still appear as skipped tests in the Surefire report
- [ ] #3 examples/README.md coverage matrix is verified against the capability files by a test
- [ ] #4 A variant declaring an unknown capability or a gap without a task ID fails the build
<!-- AC:END -->
