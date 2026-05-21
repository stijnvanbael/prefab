---
id: TASK-221
title: Tighten union mother @Example branch disambiguation
status: To Do
assignee: []
created_date: ''
updated_date: '2026-05-21 06:22'
labels: []
dependencies: []
ordinal: 144000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Union wrappers generated from AVSC fields currently use `@Example` value matching by trying permitted branch value types.
This works for straightforward cases (for example string-only matches), but ambiguous literals (for example `"1"`
matching both numeric and string branches) still rely on implicit subtype order.

Add explicit and deterministic branch disambiguation so generated mother defaults are predictable and configurable.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Support explicit branch selection for union `@Example` values (for example via metadata key or structured format)
- [ ] #2 Keep backward compatibility for existing plain scalar `@Example` values
- [ ] #3 Emit a clear compiler error when a value matches multiple branches without disambiguation
- [ ] #4 Emit a clear compiler error when a value matches no permitted branch
- [ ] #5 Add AVSC + mother generation tests covering ambiguous and explicit-branch examples
- [ ] #6 Document the branch-selection format in `backlog/docs/generated-artefacts.md`
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Created as follow-up while implementing union mother generation for permitted branch wrappers and `@Example` matching.
- Scope is limited to union branch disambiguation and diagnostics, not general mother defaults.
<!-- SECTION:NOTES:END -->
