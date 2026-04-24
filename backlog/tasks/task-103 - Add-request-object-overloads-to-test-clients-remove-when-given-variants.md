---
id: TASK-103
title: 'Add request object overloads to test clients, remove when/given variants'
status: To Do
assignee: []
created_date: '2026-04-01 17:11'
updated_date: '2026-04-24 06:57'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 126000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The generated test client methods (e.g. createPerson, updatePerson) currently accept individual field parameters and also expose whenCreating/givenCreated convenience aliases. Adding a request-object overload makes it easy to pass a pre-built request (e.g. from an Object Mother) directly to the client method, while the individual-parameter variant continues to exist as the primary form. The when/given aliases add no value once the overload is present and should be removed to keep the API surface minimal.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 For every create method in the generated test client, add an overloaded variant that accepts the corresponding CreateRequest object directly (e.g. createPerson(CreatePersonRequest request)) in addition to the existing individual-parameter variant
- [ ] #2 For every update method in the generated test client, add an overloaded variant that accepts the corresponding request object directly (e.g. updatePerson(String id, PersonUpdateRequest request)) in addition to the existing individual-parameter variant
- [ ] #3 Remove the whenCreating{Aggregate} and given{Aggregate}Created method variants from CreateTestClientWriter
- [ ] #4 Remove any equivalent when/given variants from other TestClientWriter classes (update, etc.) if they exist
- [ ] #5 The overloaded request-object methods delegate to the same HTTP call logic as the individual-parameter methods (no duplication of MockMvc call code)
- [ ] #6 Existing test client unit tests are updated to cover the new overloaded methods and to remove assertions on the deleted when/given methods
- [ ] #7 No changes are needed to the controller or service writers - only test client generation is affected
<!-- AC:END -->
