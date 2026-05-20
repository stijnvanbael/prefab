---
id: TASK-221
title: Tighten union mother @Example branch disambiguation
status: To Do
assignee: []
created: 2026-05-20
updated: 2026-05-20
---

## Description

Union wrappers generated from AVSC fields currently use `@Example` value matching by trying permitted branch value types.
This works for straightforward cases (for example string-only matches), but ambiguous literals (for example `"1"`
matching both numeric and string branches) still rely on implicit subtype order.

Add explicit and deterministic branch disambiguation so generated mother defaults are predictable and configurable.

## Acceptance criteria

- [ ] Support explicit branch selection for union `@Example` values (for example via metadata key or structured format)
- [ ] Keep backward compatibility for existing plain scalar `@Example` values
- [ ] Emit a clear compiler error when a value matches multiple branches without disambiguation
- [ ] Emit a clear compiler error when a value matches no permitted branch
- [ ] Add AVSC + mother generation tests covering ambiguous and explicit-branch examples
- [ ] Document the branch-selection format in `backlog/docs/generated-artefacts.md`

## Implementation notes

- Created as follow-up while implementing union mother generation for permitted branch wrappers and `@Example` matching.
- Scope is limited to union branch disambiguation and diagnostics, not general mother defaults.

