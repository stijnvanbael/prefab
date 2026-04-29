---
id: TASK-123
title: 'Document aggregate, event and value object fields'
status: Done
assignee: []
created_date: '2026-04-17 00:00'
updated_date: '2026-04-29 14:53'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 16000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow developers to annotate fields on aggregates, events and value objects with documentation that is
automatically propagated to all generated API and schema artefacts.

Currently, field-level descriptions are absent from the generated outputs, making it harder for API consumers
to understand the meaning and constraints of individual fields.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `@Doc` annotation (or equivalent) can be placed on any field of an aggregate, event or value object
- [ ] #2 Field descriptions are included in generated **Open API** (Swagger) schemas as the `description` property
- [ ] #3 Field descriptions are included in generated **Async API** schemas as the `description` property
- [ ] #4 Field descriptions are included in generated **Avro AVSC** schemas as the `doc` property
- [ ] #5 Fields without a description annotation produce no description property in the output (no empty strings)
- [ ] #6 Annotation processor picks up field-level `@Doc` values alongside existing class-level processing
- [ ] #7 End-to-end test or example module verifies that field docs appear correctly in all three outputs
<!-- AC:END -->



## Notes

- Reuse or extend the existing documentation annotation if one already exists in the annotation-processor module
- Keep consistency with how class-level `@Doc` / description annotations are already handled
- Avro uses `"doc"` key; Open API and Async API both use `"description"` key
