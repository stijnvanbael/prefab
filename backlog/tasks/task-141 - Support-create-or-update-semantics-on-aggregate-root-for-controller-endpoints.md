---
id: TASK-141
title: Support create-or-update semantics on aggregate root for controller endpoints
status: To Do
assignee: []
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
Controller endpoints should support a create-or-update scenario, mirroring the event-handler variant introduced in TASK-121. A static method on the aggregate root annotated with @CreateOrUpdate (with a 'property' attribute mapping to the request field) should accept an Optional<AggregateType> (empty when not found) and the request object, and return the saved aggregate. The framework looks up the existing aggregate by the specified request property and always saves the result, generating a single PUT endpoint instead of separate @Create / @Update endpoints.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A static method annotated with @CreateOrUpdate on an aggregate root is discovered by a new CreateOrUpdateControllerPlugin
- [ ] #2 Plugin validates the static method signature: two parameters (Optional<AggregateType>, RequestType) and return type AggregateType
- [ ] #3 A PUT endpoint is generated that looks up the aggregate by the request property specified in @CreateOrUpdate, passes Optional<AggregateType> and the request to the static method, and saves the result
- [ ] #4 No separate @Create or @Update endpoint is generated for an aggregate root that uses @CreateOrUpdate
- [ ] #5 Plugin is registered in META-INF/services alongside existing controller plugins
- [ ] #6 Test source files added for the create-or-update controller scenario (aggregate + request)
- [ ] #7 Tests verify: generated PUT endpoint, create path (empty Optional), update path (present Optional), and correct save call
<!-- AC:END -->
