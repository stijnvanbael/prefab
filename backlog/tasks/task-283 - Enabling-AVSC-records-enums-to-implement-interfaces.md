---
id: TASK-283
title: Enabling AVSC records/enums to implement interfaces
status: In Progress
assignee: []
created_date: '2026-09-10 09:42'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow individual AVSC-generated records or enums to implement an interface.

Requirements:
- Enable interface specification on the top-level event interface in the event hierarchy
- Match generated types to interfaces by namespace and name
- Support multiple records/enums implementing the same interface
- Generated code should declare the interface implementation

Acceptance Criteria:
- AVSC configuration accepts interface specifications (e.g. via annotation or metadata)
- Code generator matches types by namespace + name and adds implements clause
- Generated records and enums properly implement the specified interface
- Interface methods are available on generated types
- All existing tests pass with interface implementations
- Documentation updated with usage examples
<!-- SECTION:DESCRIPTION:END -->

## Analysis

- `AvscPlugin` currently forwards only the top-level `@Avsc` contract interface to `AvscEventWriter`, so generated top-level records implement that contract but generated nested records and enums never implement any additional user-specified interfaces.
- `AvscEventWriter` already owns top-level record, nested record, and nested enum generation, so it is the right place to add interface implementation support consistently across all named AVSC types.
- Matching must use the original Avro `namespace` + `name`, not the generated Java package or simple name, because Prefab keeps generated types in the contract package and may capitalise AVSC names for Java output.
- A repeatable contract-level annotation is the smallest explicit API that allows multiple generated records/enums to share one Java interface while keeping the mapping close to the top-level AVSC event contract.

## Progress

- Reviewed `TASK-283`, the AVSC developer guide sections, `AvscPlugin`, `AvscEventWriter`, and the current AVSC regression tests to identify the minimal extension points.
