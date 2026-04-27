---
id: TASK-143
title: Nullable String fields not generated as nullable in Avro schema
status: To Do
assignee: []
created_date: '2026-04-27 14:07'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When a String field is annotated with @Nullable, the generated Avro schema does not wrap it in a nullable union schema. The EventSchemaFactoryWriter checks for @Nullable in buildFieldBlock, but createSchema dispatches String to createPrimitiveSchema before that nullable wrapping can apply correctly.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Nullable String fields produce a nullable union schema (e.g. [null, string])
- [ ] #2 Non-nullable String fields are unaffected
- [ ] #3 A test covers the nullable String schema generation
<!-- AC:END -->
