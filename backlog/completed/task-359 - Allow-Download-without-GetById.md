---
id: TASK-359
title: Allow @Download without @GetById
status: Done
assignee: []
created_date: '2026-11-21 18:10'
labels:
  - rest
dependencies: []
priority: low
type: bug
ordinal: 198000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The generated download endpoint of a `@Download` field calls `service.getById(id)`, which the service only has when the
aggregate is annotated `@GetById`. Without it, the generated controller does not compile ("cannot find symbol
getById"). Either generate the lookup the download needs, or report a clear compile error on the aggregate.

Found while writing an annotation processor fixture for TASK-337.03.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 An aggregate with @Download but without @GetById compiles, or fails with an error that names the missing annotation
<!-- AC:END -->

## Implementation Notes

Chose the clear-compile-error option: `BinaryPlugin.writeController` reports
`[@Download] field '<name>' on <Aggregate> requires the aggregate to be annotated with @GetById` and skips the
download endpoint (the test client has no lookup dependency, so it is unaffected). Covered by
`DownloadWithoutGetByIdTest`; documented under `@Download` in `annotation-reference.md`.
