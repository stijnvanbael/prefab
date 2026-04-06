---
id: TASK-011
title: Generic value types
status: In Progress
assignee:
  - '@agent'
created_date: '2025-10-10 13:34'
updated_date: '2026-04-06 05:54'
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
- [ ] #1 DataType.typeOf() delegates single-value types to their inner field type (e.g. Amount(BigDecimal) -> DECIMAL)
- [ ] #2 RequestParameterBuilder uses the inner field type instead of String for single-value types
- [ ] #3 GetListServiceWriter maps single-value filter params to inner field type
- [ ] #4 GetListControllerWriter maps single-value filter params to inner field type
- [ ] #5 Tests cover non-String single-value types in DB migration and REST
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
