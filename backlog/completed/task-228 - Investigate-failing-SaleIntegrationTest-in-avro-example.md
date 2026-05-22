---
id: TASK-228
title: Investigate failing SaleIntegrationTest in avro example
status: Done
assignee: []
created_date: '2026-05-22 05:47'
updated_date: '2026-05-22 05:54'
labels:
  - tests
  - avro
  - investigation
dependencies: []
references:
  - >-
    examples/avro/src/test/java/be/appify/prefab/example/avro/sale/SaleIntegrationTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate why examples/avro sale integration test fails and identify root cause with evidence.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Reproduce failure locally and capture the exact failing assertion or exception.
- [x] #2 Identify root cause in code or test setup with file references.
- [x] #3 Provide a concrete fix recommendation (and implement if requested).
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Reproduced with `mvn -pl examples/avro -Dtest=SaleIntegrationTest test -DtrimStackTrace=false`.

Failure: Awaitility timeout in SaleIntegrationTest.sale line 38; assertion line 40 expected cash drawer 107.5 but remained 100.0.

Root cause is Avro event-name mismatch in generated code: schema factories emit names like `Sale_Paid` while generic-record converter switches on `Sale.Paid`, so paid event is not converted and consumer update never runs.

Likely generator bug in avro-processor: GenericRecordToEventConverterWriter uses simpleName-based case labels; EventSchemaFactoryWriter normalizes names with dot-to-underscore conversion.

Recommended fix: align converter case labels to the same Avro naming strategy (including AvroSchema name overrides), regenerate sources, rerun test.
<!-- SECTION:NOTES:END -->
