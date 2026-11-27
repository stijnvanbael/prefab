---
id: TASK-369
title: Do not apply the single AVSC file to events that are not defined by it
status: Done
assignee: []
created_date: '2026-11-23 17:00'
labels:
  - annotation-processor
  - avro
dependencies: []
priority: low
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while building the showcase Avro slice (TASK-337.07), which mixes an AVSC-first event with code-first Avro events.

`EventSchemaFactoryWriter.findAvscPath` falls back to the only AVSC file of the module (`index.size() == 1`) for every event it cannot match by name. A code-first Avro event (`PaymentEvent.Received`) is therefore treated as a participant of the unrelated AVSC file: it gets the AVSC namespace (`be.appify.prefab.example.showcase.shipping` instead of its own package) and single-value records are preserved as for AVSC events. The generated code still works because writer and reader agree, but the Avro names are wrong and the schema registry shows a namespace that has nothing to do with the event.

Reproduce: `examples/showcase/src/avro/java` and read `PaymentEventReceivedSchemaFactory` in `target/generated-sources/annotations`.

Restrict the fallback to events that really belong to the AVSC (nested in or implementing an `@Avsc` interface), and add a processor test that mixes a code-first and an AVSC-first Avro event in one compilation and asserts the namespace of the code-first one.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A code-first Avro event in a module with one AVSC file keeps the namespace of its own package
- [x] #2 A processor test covers the mixed case
<!-- AC:END -->

## Implementation Notes

`EventSchemaFactoryWriter.findAvscPath()` now applies the single-file fallback only to types related to an `@Avsc` contract (an implementation or a type nested in/under that contract). Added a mixed compile regression test proving an unrelated code-first event keeps its Java package as the Avro namespace. Updated the Avro guide, cheat sheet, publishing recipe, and the Avro publish-and-handle benchmark to cover the mixed case.

Verification: `mvn -pl avro-processor -am test` and `mvn -pl benchmark -am "-Dtest=RecipeDocumentationTest,WorkspaceFactoryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` both passed.
