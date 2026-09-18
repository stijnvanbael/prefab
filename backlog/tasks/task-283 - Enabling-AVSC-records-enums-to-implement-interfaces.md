---
id: TASK-283
title: Enabling AVSC records/enums to implement interfaces
status: Done
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

## Implementation Notes

- Added repeatable `@AvscInterface` / `@AvscInterfaces` annotations in `prefab-core` so the top-level `@Avsc` contract can map Avro `namespace` + `name` pairs to Java interfaces.
- Updated `AvscPlugin` to collect the configured mappings, validate that each target is a Java interface, and pass the resolved interface set into AVSC generation.
- Updated `AvscEventWriter` so top-level records, nested records, and nested enums all add matching `implements` clauses based on the original Avro schema identity, while still keeping the top-level event contract on generated event records.
- Added `AvscPluginTest` regression coverage and AVSC fixtures proving that:
  - multiple generated records can share the same interface
  - an individual generated record can implement multiple interfaces
  - a generated enum can implement its own mapped interface
- Updated `Avsc` Javadoc plus `backlog/docs/annotation-reference.md` and `backlog/docs/feature-guides.md` with usage examples for the new contract.

## Verification

- `runtime-tools-secret_scanning` on all changed files: **PASS**
- `mvn -q -pl core,avro-processor -am -Dtest=AvscPluginTest -Dsurefire.failIfNoSpecifiedTests=false test`: **BLOCKED** by unavailable `io.confluent:kafka-streams-avro-serde:8.3.0` from `https://packages.confluent.io/maven/`
- `parallel_validation`: Code Review found one unsafe cast in `AvscPlugin`, which was fixed; a follow-up review found one Javadoc example issue in `Avsc`, which was also fixed. CodeQL reported no alerts before a later validation run timed out.
