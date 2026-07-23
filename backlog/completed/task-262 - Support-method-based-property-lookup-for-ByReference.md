---
id: TASK-262
title: Support method-based property lookup for @ByReference
status: Done
assignee: []
created_date: '2026-07-10 09:49'
updated_date: '2026-07-10 10:00'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The ByReferenceEventHandlerPlugin currently only scans FIELD elements of the event type when resolving the @ByReference(property = ...) attribute. If the event exposes its reference value via a zero-arg method (e.g. a getter or a record accessor that is not directly a field), the lookup fails with a misleading compile-time error message. Two issues need to be fixed: 1. Add method-based lookup as a fallback alongside field lookup. 2. Correct the error message which incorrectly says the property must be of type Reference instead of stating the actual constraint (primitive or single-value type).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 When @ByReference(property = X) is used, the plugin resolves X against both fields and zero-arg methods of the event type
- [ ] #2 If no matching field or method is found, the error message reads: event type X does not have a field or method named Y, or it is not of a primitive or single-value type
- [ ] #3 An event that exposes its reference value via a zero-arg method compiles successfully and generates the correct handler code calling that method
- [ ] #4 Fields take precedence over methods when both share the same name
- [ ] #5 The @ByReference Javadoc and developer guide are updated to reflect that property may name either a field or a zero-arg method
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented in ByReferenceEventHandlerPlugin: added getAccessors() that scans zero-arg public non-static methods of the event type via VariableManifest.ofMethod(). The byReferenceEventHandlers() stream now uses Stream.concat(getFields(), getAccessors()), so fields take precedence but methods are also resolved. Error message updated to accurately describe the constraint. Two test fixtures added (byreferencemethodproperty/source/) and two new test methods in EventHandlerWriterTest verify compilation success and correct generated code.
<!-- SECTION:NOTES:END -->
