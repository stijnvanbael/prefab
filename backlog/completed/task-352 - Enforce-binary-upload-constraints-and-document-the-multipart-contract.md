---
id: TASK-352
title: Enforce binary upload constraints and document the multipart contract
status: Done
assignee: []
created_date: '2026-11-21 09:45'
labels:
  - rest
  - bug
  - documentation
  - agents
dependencies: []
priority: high
type: bug
ordinal: 197000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Benchmark task `binary-upload-download` (TASK-343) showed two problems with `Binary` fields:

1. **`@ContentType` and `@FileSize` are not enforced.** The generated `Create...Request` drops them, and the controller attaches the file with `request.withFile(file)` after bean validation has already run. Any file type and size is accepted, even though the feature guide (7.7) and the annotation reference document these constraints.
2. **The documented multipart contract is wrong.** The docs say `POST /documents` takes a multipart form with a `title` field and a `file` part. The generated controller actually expects a JSON part named `body` with the other fields, plus the file part. A request in the documented format fails with `MissingServletRequestPartException`, which surfaces as `500` instead of `400`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Uploads that violate @ContentType or @FileSize are rejected with 400 and a clear problem detail
- [x] #2 A missing or malformed multipart part results in 400, not 500
- [x] #3 The feature guide and generated-artefacts docs describe the actual multipart request format, with an example
- [x] #4 Covered by tests; the benchmark task binary-upload-download reference no longer needs its manual validation
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Constraint enforcement: `BinaryPlugin.requestMethodParameter` copies `@ContentType`/`@FileSize` from the `@Create`/`@Update`
  parameter onto the generated `MultipartFile` controller parameter, so Spring method validation rejects a violating
  upload with 400 before it is stored (the update path was already covered by the showcase test). Added
  `CreateBinaryTest` to pin the same behaviour for `@Create`.
- Missing/malformed parts: the error-handling starter answered 500 for `MissingServletRequestPartException`. New
  `MultipartExceptionHandler` (core) maps it and `MultipartException` to 400 (`INVALID_MULTIPART_REQUEST`);
  `MaxUploadSizeExceededException` keeps its default status.
- Docs: multipart format with `curl` example in generated-artefacts 6.1 and feature guide 7.7; annotation reference,
  troubleshooting updated.
- Benchmark `binary-upload-download`: reference no longer validates manually, `task.yml` notes removed, hidden test for
  a missing file part added. Not run here (needs Docker/Testcontainers).
<!-- SECTION:NOTES:END -->
