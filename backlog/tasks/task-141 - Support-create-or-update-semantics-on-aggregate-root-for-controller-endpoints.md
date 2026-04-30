---
id: TASK-141
title: Support create-or-update semantics on aggregate root for controller endpoints
status: To Do
assignee: [ ]
created_date: '2026-04-27 13:13'
updated_date: '2026-04-27 13:14'
labels:
  - annotation-processor
  - rest
dependencies:
  - TASK-121
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Controller endpoints should support a create-or-update scenario, mirroring the event-handler variant introduced in
TASK-121. A @Create constructor or static factory can share the same URL and HTTP method with an @Update method. The
framework looks up the existing aggregate by the specified request property, delegates to the create or update method
depending on the result, and always saves the result, generating a single endpoint instead of separate @Create / @Update
endpoints.
<!-- SECTION:DESCRIPTION:END -->