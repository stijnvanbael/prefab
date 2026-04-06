---
id: TASK-011
title: Generic value types
status: Done
assignee:
  - '@agent'
created_date: '2025-10-10 13:34'
updated_date: '2026-04-06 06:02'
labels: []
dependencies: []
ordinal: 7000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Support value types (single-field records) with non-String field types in the annotation processor. Currently, all single-value types are treated as String regardless of their actual field type. This affects: DB migration (always generates VARCHAR instead of correct type), REST parameters (always mapped to String instead of actual type), and GetList filter handling.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 DataType.typeOf() delegates single-value types to their inner field type (e.g. Amount(BigDecimal) -> DECIMAL)
- [x] #2 RequestParameterBuilder uses the inner field type instead of String for single-value types
- [x] #3 GetListServiceWriter maps single-value filter params to inner field type
- [x] #4 GetListControllerWriter maps single-value filter params to inner field type
- [x] #5 Tests cover non-String single-value types in DB migration and REST
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Fix DataType.typeOf() to delegate single-value types to inner field type
2. Fix RequestParameterBuilder.defaultParameters() to use inner field type
3. Fix GetListServiceWriter to use inner field type for filter params
4. Fix GetListControllerWriter to use inner field type for filter params
5. Add test for non-String single-value types in DB migration
6. Add test for non-String single-value types in REST
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Fixed DataType.typeOf() to delegate single-value types to their inner field type, so Amount(BigDecimal) correctly maps to DECIMAL(19,4) instead of VARCHAR(255)
- Fixed RequestParameterBuilder.defaultParameters() to use the actual inner field type as the effective parameter type instead of hardcoded String
- Fixed GetListServiceWriter and GetListControllerWriter to use inner field type for filter parameters via new filterParamType() helper
- Added DB migration test: nonStringValueTypesMappedToCorrectColumnType()
- Added REST test: nonStringValueTypeParametersUseInnerFieldType()
- All existing tests are unaffected: Reference<T> (String id) still correctly maps to VARCHAR(255) and String parameters
<!-- SECTION:NOTES:END -->
