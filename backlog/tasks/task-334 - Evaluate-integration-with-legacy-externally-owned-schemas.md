---
id: TASK-334
title: Evaluate integration with legacy externally owned schemas
status: To Do
assignee: []
created_date: '2026-11-18 10:39'
labels:
  - spike
  - persistence
  - architecture
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Assess a Prefab application that reads and updates an externally managed PostgreSQL schema or calls a stored procedure without taking ownership of schema migrations. Compare repository mixins and handwritten Spring Data adapters with the planned provider SPIs in TASK-180; do not assume these SPIs solve arbitrary legacy mapping. Related: TASK-309 and decision-006.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Demonstrate one bounded read and write or stored-procedure workflow against a representative externally owned schema, without automatic schema changes, and compare the existing escape hatch with any proposed extension.
- [ ] #2 Specify identity and concurrency, local transaction ownership, tenant/row authorization and parameterized access; make unsupported mappings or schema assumptions explicit.
- [ ] #3 Exercise mapping, tenant isolation, concurrent changes and procedure or database failures with tests, and show that errors are not converted into successful results.
- [ ] #4 Document generated versus handwritten responsibilities, provider and migration boundaries, compatibility with TASK-180, and a go/no-go recommendation for any new extension.
<!-- AC:END -->
