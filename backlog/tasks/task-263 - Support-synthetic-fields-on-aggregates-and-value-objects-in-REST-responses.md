---
id: TASK-263
title: Support synthetic fields on aggregates and value objects in REST responses
status: To Do
assignee: []
created_date: '2026-07-10 11:31'
labels:
  - feature-request
dependencies: []
priority: medium
---

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new annotation (e.g. @Computed) can be placed on a no-arg method of an aggregate root or value object
- [ ] #2 The annotated method return value is included as a read-only field in the generated REST response DTO
- [ ] #3 The synthetic field is not present in request DTOs (create / update)
- [ ] #4 The synthetic field is not mapped to a database column
- [ ] #5 The field name in the response matches the method name
<!-- AC:END -->
