---
id: TASK-231
title: Mother generator ignores @Example on inner field of single-value type
status: Done
assignee: []
created_date: '2026-05-26 11:29'
updated_date: '2026-05-26 13:24'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When a record field is a single-value type (a wrapper record with one field), and the inner field of that wrapper carries an @Example annotation, the generated Object Mother ignores the example and falls back to the type default value instead. Root cause: in MotherWriter.typeDefaultValue(), the isSingleValueType() branch calls typeDefaultValue() recursively on the inner field, bypassing defaultValueFor() which is the only method that consults @Example. Fix: replace the recursive typeDefaultValue() call with defaultValueFor() so that @Example on the inner field is honoured.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 When a record field is a single-value type whose inner field has @Example, the generated mother uses the example value
- [x] #2 The fix does not break the default behaviour when no @Example is present
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed in MotherWriter.java with two co-operating changes:

1. **`typeDefaultValue()` (event-mother path)** – the `isSingleValueType()` branch now calls `defaultValueFor(innerField, innerField.type().asBoxed())` instead of the raw `typeDefaultValue(...)` recursive call, so any `@Example` on the inner field is consulted before falling back to the structural default.

2. **`defaultValueFor()` (request-mother path)** – added a new `innerExampleOf()` helper that runs between the outer-param `@Example` check and the `typeDefaultValue()` fallback. When the outer param has no `@Example` but its declared type is a single-value wrapper, the helper reads `@Example` from the wrapper's inner field and forwards the value to `exampleLiteralFor()` against the effective (unwrapped) type.

The request-mother path needs the second fix because `effectiveTypeOf()` strips the wrapper type before `defaultValueFor()` is called, so by the time the old code checked for examples it no longer knew the original type was a single-value wrapper.

Regression test added: `Invoice` aggregate with an `InvoiceNumber` single-value type whose inner field is `@Example("INV-001") String value`. The generated `CreateInvoiceRequestMother` now contains `"INV-001"` instead of the field-name default `"invoiceNumber"`. All 307 tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
