---
id: TASK-168
title: Improve annotation-processor compilation performance (general)
status: Done
assignee:
  - '@copilot'
created_date: '2026-05-08 11:20'
updated_date: '2026-05-08 11:27'
labels:
  - performance
  - annotation-processor
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Analysis of the annotation processor revealed several performance bottlenecks unrelated to AVSC events that scale poorly with the number of aggregates and types processed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 TypeMembers.fields() lazily memoizes its result; repeated calls to type.fields() (e.g. from isSingleValueType(), annotation checks) on the same TypeManifest do not re-traverse the element hierarchy
- [x] #2 TypeAnnotations.inheritedAnnotationsOfType() caches results per annotation type; the recursive supertype walk is not repeated for the same query on the same type
- [x] #3 ClassManifest.constructorsWith(), methodsWith(), and staticMethodsWith() results are memoized; repeated calls during code generation do not re-scan typeElement.getEnclosedElements()
- [x] #4 TypeIdentity.is() compares against a precomputed final FQN field instead of reconstructing the string on every call
- [x] #5 JavaFileWriter replaces the synchronized WeakHashMap with a plain HashMap; synchronization overhead is eliminated given annotation processing is single-threaded
- [x] #6 AvroPlugin.allNestedTypes() does not redundantly re-expand sealedSubtypes() per BFS iteration; sealed subtype expansion is computed once per type
- [x] #7 All existing annotation-processor and avro-processor tests continue to pass after the changes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Precompute FQN field in TypeIdentity.is() (AC-4)
2. Lazy-memoize TypeMembers.fields() result (AC-1)
3. Cache TypeAnnotations.inheritedAnnotationsOfType() per annotation type (AC-2)
4. Memoize ClassManifest.constructorsWith/methodsWith/staticMethodsWith (AC-3)
5. Replace synchronized WeakHashMap in JavaFileWriter with plain HashMap (AC-5)
6. Fix AvroPlugin.allNestedTypes() BFS to avoid redundant sealedSubtypes() re-expansion (AC-6)
7. Run tests to verify all pass (AC-7)
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- TypeIdentity.is(): precomputed `fqn` final field in constructor — eliminates per-call string allocation in the hottest path of the processor
- TypeMembers.fields(): lazy-initialised `cachedFields` field — avoids re-traversing the element hierarchy on every call to type.fields()
- TypeAnnotations.inheritedAnnotationsOfType(): results cached in a HashMap keyed by annotation type — recursive supertype walk runs at most once per annotation per type
- ClassManifest.constructorsWith/methodsWith/staticMethodsWith: results memoized in per-instance HashMaps — repeated enclosed-element scans during code generation eliminated
- Removed dead clearCache() methods from ClassManifest and VariableManifest (dead code left by TASK-167)
- JavaFileWriter: replaced synchronized WeakHashMap with plain HashMap — annotation processing is single-threaded; no locking needed
- AvroPlugin.allNestedTypes(): BFS now expands fields of a type plus its own sealed subtypes directly, rather than re-invoking nestedTypes() which redundantly re-expanded all ancestors sealed subtypes on every iteration
- All annotation-processor and avro-processor tests pass
<!-- SECTION:NOTES:END -->
