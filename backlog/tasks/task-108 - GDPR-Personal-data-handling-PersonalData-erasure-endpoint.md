---
id: TASK-108
title: 'GDPR / Personal data handling (@PersonalData, erasure endpoint)'
status: To Do
assignee: []
created_date: '2026-04-09 15:29'
updated_date: '2026-04-24 06:57'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 129000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The General Data Protection Regulation (GDPR) and similar privacy laws (CCPA, LGPD) require organisations to fulfil "right to erasure" (right to be forgotten) requests, respond to Subject Access Requests (SARs), and document exactly which fields contain personal data. Non-compliance carries fines of up to 4 % of global turnover.

Prefab should reduce the compliance burden by providing a `@PersonalData` annotation that marks sensitive fields and drives:
1. A generated `/data-subject/{subjectId}/erasure` endpoint that anonymises or deletes all personal data linked to the specified subject.
2. A generated `/data-subject/{subjectId}/export` endpoint (Subject Access Request) that collects all personal data linked to the subject and returns it as JSON.
3. Compile-time documentation of which fields are marked personal so teams have an automatic data map.

Example usage:

```java
@Aggregate
@GetById
public record Customer(
    @Id Reference<Customer> id,
    @Version long version,
    @PersonalData(subjectId = true) String email,   // links the record to the data subject
    @PersonalData String fullName,
    @PersonalData String phoneNumber,
    String accountTier                              // not personal
) { ... }
```

Fields marked `subjectId = true` are used to locate all records for a given data-subject identifier (could be an email or a user-id). Erasure replaces personal field values with a configurable placeholder (default `"[REDACTED]"` for strings, `null` for nullable types). An optional `strategy = ErasureStrategy.DELETE` attribute deletes the entire aggregate instead of anonymising it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add @PersonalData(subjectId = false, strategy = ErasureStrategy.ANONYMISE) annotation to prefab-core; ErasureStrategy enum has values ANONYMISE and DELETE
- [ ] #2 The annotation processor detects all aggregates that carry at least one @PersonalData-annotated field and generates a DataSubjectService Spring component that exposes eraseDataSubject(String subjectId) and exportDataSubject(String subjectId) methods
- [ ] #3 Generate a DELETE /data-subject/{subjectId}/erasure REST endpoint (via a new DataSubjectController) that invokes DataSubjectService.eraseDataSubject; the endpoint is secured by a configurable role (default ROLE_DATA_PROTECTION_OFFICER) configurable via prefab.gdpr.erasure-role property
- [ ] #4 Generate a GET /data-subject/{subjectId}/export REST endpoint that invokes DataSubjectService.exportDataSubject and returns a JSON map of aggregate type → list of matching records containing only @PersonalData fields
- [ ] #5 ANONYMISE strategy replaces String fields with the value configured in prefab.gdpr.redaction-placeholder (default [REDACTED]) and sets nullable fields to null; DELETE strategy removes the entire aggregate record
- [ ] #6 The annotation processor emits a PersonalDataReport – a generated Markdown file (META-INF/personal-data-report.md) listing every aggregate, field, and its @PersonalData annotation metadata, to serve as a lightweight data map for compliance documentation
- [ ] #7 A compile error is raised if @PersonalData(subjectId=true) is absent from an aggregate that has other @PersonalData fields (there must be exactly one subject-id field per aggregate)
- [ ] #8 Add annotation-processor unit tests and an integration test for the erasure and export endpoints following the pattern of existing tests
- [ ] #9 README updated with a 'GDPR / Personal data' section documenting the annotations, generated endpoints, ErasureStrategy options, and configuration properties
<!-- AC:END -->
