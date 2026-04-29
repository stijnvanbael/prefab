---
id: TASK-143
title: >-
  Nullable String fields not generated as nullable in Avro schema generated from
  AVSC
status: In Progress
assignee: []
created_date: '2026-04-27 14:07'
updated_date: '2026-04-29 14:52'
labels: []
dependencies: []
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Only observed when generating Avro schemas from AVSC files, not when generating from Java classes.
When a String field is annotated with @Nullable, the generated Avro schema does not wrap it in a nullable union schema.
The EventSchemaFactoryWriter checks for @Nullable in buildFieldBlock, but createSchema dispatches String to
createPrimitiveSchema before that nullable wrapping can apply correctly.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Nullable String fields produce a nullable union schema (e.g. [null, string])
- [ ] #2 Non-nullable String fields are unaffected
- [ ] #3 A test covers the nullable String schema generation
- [ ] #4 Schema generation works correctly for both AVSC and Java source producing the same result for nullable String
  fields
<!-- AC:END -->
