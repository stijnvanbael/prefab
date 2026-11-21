---
id: TASK-352
title: Enforce binary upload constraints and document the multipart contract
status: To Do
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
- [ ] #1 Uploads that violate @ContentType or @FileSize are rejected with 400 and a clear problem detail
- [ ] #2 A missing or malformed multipart part results in 400, not 500
- [ ] #3 The feature guide and generated-artefacts docs describe the actual multipart request format, with an example
- [ ] #4 Covered by tests; the benchmark task binary-upload-download reference no longer needs its manual validation
<!-- AC:END -->
