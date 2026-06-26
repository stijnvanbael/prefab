---
id: TASK-256
title: Detect cross-AVSC record incompatibilities in AvscPlugin
status: Done
assignee: []
created_date: '2026-06-24 08:24'
updated_date: '2026-06-24 09:12'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The AvscPlugin currently processes each AVSC file in isolation. When the same named record (identified by its fully-qualified Avro name: namespace + record name) appears in multiple AVSC files, the processor silently generates duplicate Java types or uses whichever definition it encounters first. This creates a hidden data-contract hazard: two event interfaces may carry structurally different definitions of a shared nested record, which breaks Avro binary compatibility at runtime.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A cross-AVSC record registry is built during AvscPlugin.writeEventFiles(), mapping each fully-qualified Avro name (namespace.RecordName) to the AVSC path where it was first defined.
- [x] #2 When the same fully-qualified record name appears in a second AVSC file with a different binary schema (field names, field types, field order, or nullability differ), the processor emits a Diagnostic.Kind.ERROR and compilation fails.
- [x] #3 The error message names both conflicting AVSC paths and the offending fully-qualified record name so the developer knows exactly where the conflict is.
- [x] #4 When the same fully-qualified record name appears in a second AVSC file but the schemas are binary-identical, a Diagnostic.Kind.WARNING is emitted only if the doc strings or sample values differ between the two definitions.
- [x] #5 The warning message identifies which doc or sample property differs, and in which two AVSC files, so the developer can align them.
- [x] #6 Binary-compatibility is determined by comparing field names, field types (including nested record structures and union variants), field order, and nullability; doc, default values, and aliases are excluded from the binary check.
- [x] #7 The check covers all named types in each AVSC file, not just the top-level record: nested records and enums reused across AVSC files are also validated.
- [x] #8 Two new AvscPluginTest cases are added: one that triggers a compile error for a binary-incompatible duplicate record, and one that triggers a warning for a binary-compatible record with mismatched doc or sample.
- [x] #9 All existing avro-processor tests continue to pass without modification.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. New AvscRecordRegistry class (round-scoped): Map<FQ-avro-name, path+Schema>; registerAll() recurses named types; SchemaNormalization.toParsingForm() for binary check; ERROR on incompatible; WARNING on doc/sample divergence only.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented AvscRecordRegistry (package-private) that builds a round-scoped map of FQ Avro name to first-seen path+Schema. SchemaNormalization.toParsingForm() is used for binary compatibility comparison (excludes doc/defaults/aliases as required). ERROR emitted when PCF differs; WARNING emitted for doc or sample divergence on binary-identical schemas. AvscPlugin.writeEventFiles() creates one registry per round and calls registerAll() per path before delegating to AvscEventWriter. collectNamedTypesInto() logic is intentionally duplicated from AvscEventWriter (shared helper refactor tracked in TASK-126). Two new test fixtures added (crossincompatible, crossdocwarning) with 2 new AvscPluginTest cases. All 64 tests pass.
<!-- SECTION:NOTES:END -->
