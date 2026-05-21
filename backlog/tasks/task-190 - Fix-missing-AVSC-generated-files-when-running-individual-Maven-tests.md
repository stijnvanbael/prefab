---
id: TASK-190
title: Fix missing AVSC generated files when running individual Maven tests
status: Done
assignee: []
created_date: '2026-05-11 08:33'
updated_date: '2026-05-21 06:21'
labels:
  - avro
  - tests
  - maven
dependencies: []
references:
  - /Users/stijnvanbael/IdeaProjects/appify/prefab
priority: high
ordinal: 35200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate and fix failures where individual Maven test runs cannot find many AVSC-generated files/events. Ensure generated test sources/resources are available for single-test execution and not only full test-suite runs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Reproduce the missing generated files issue when running a targeted Maven test.
- [x] #2 Identify why AVSC-generated artifacts are unavailable in individual test execution.
- [x] #3 Implement a fix so targeted test runs resolve AVSC-generated artifacts reliably.
- [x] #4 Add or update tests/build configuration to guard against regression.
- [x] #5 Document any affected behavior in backlog docs if feature behavior changes.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Reproduced isolated Maven test execution path using targeted Surefire method selection for Kafka AVSC scenarios.

Updated kafka test dependency compilation helper to run Prefab annotation processing so AVSC-generated artifacts are produced for dependency classpaths.

Added regression coverage for AVSC-generated dependency event handling in KafkaConsumerWriterTest with dedicated dependency AVSC test fixtures.

Validated with isolated method run and full KafkaConsumerWriterTest class run in Maven reactor.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed isolated Maven test execution for AVSC dependency scenarios by enabling PrefabProcessor in kafka test dependency classpath compilation.

Added regression test `avscEventTypeFromDependencyModule` and supporting AVSC fixtures to verify generated dependency artifacts are resolved in targeted runs.

Verified via `mvn -pl kafka -am -Dtest=KafkaConsumerWriterTest#avscEventTypeFromDependencyModule -Dsurefire.failIfNoSpecifiedTests=false test` and `mvn -pl kafka -am -Dtest=KafkaConsumerWriterTest -Dsurefire.failIfNoSpecifiedTests=false test`.
<!-- SECTION:FINAL_SUMMARY:END -->
