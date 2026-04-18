---
id: TASK-123
title: Document aggregate, event and value object fields
status: To Do
assignee: []
created_date: '2026-04-17 00:00'
updated_date: '2026-04-17 00:00'
labels:
  - "📦feature"
dependencies: []
ordinal: 123000
---

## Description

Allow developers to annotate fields on aggregates, events and value objects with documentation that is
automatically propagated to all generated API and schema artefacts.

Currently, field-level descriptions are absent from the generated outputs, making it harder for API consumers
to understand the meaning and constraints of individual fields.

## Acceptance Criteria

- [ ] A `@Doc` annotation (or equivalent) can be placed on any field of an aggregate, event or value object
- [ ] Field descriptions are included in generated **Open API** (Swagger) schemas as the `description` property
- [ ] Field descriptions are included in generated **Async API** schemas as the `description` property
- [ ] Field descriptions are included in generated **Avro AVSC** schemas as the `doc` property
- [ ] Fields without a description annotation produce no description property in the output (no empty strings)
- [ ] Annotation processor picks up field-level `@Doc` values alongside existing class-level processing
- [ ] End-to-end test or example module verifies that field docs appear correctly in all three outputs

## Notes

- Reuse or extend the existing documentation annotation if one already exists in the annotation-processor module
- Keep consistency with how class-level `@Doc` / description annotations are already handled
- Avro uses `"doc"` key; Open API and Async API both use `"description"` key

