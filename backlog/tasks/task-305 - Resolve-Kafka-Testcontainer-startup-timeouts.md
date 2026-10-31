---
id: TASK-305
title: Resolve Kafka Testcontainer startup timeouts
status: To Do
assignee: []
created_date: '2026-10-31 13:57'
labels:
  - testing
  - kafka
dependencies: []
references:
  - backlog/tasks/task-303 - Support-generic-event-types-in-all-event-related-features.md
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Full-repository verification of TASK-303 repeatedly fails before application startup because the `apache/kafka:4.4.0` Testcontainer never satisfies the readiness check. Investigate the configured Kafka image, Testcontainers 2.0.5 and Windows/Podman compatibility without disabling integration tests or replacing meaningful readiness checks with unconditional success.

The final TASK-303 reactor reproduces this in `examples/avro` (`SaleIntegrationTest`) before any generic-topic provisioning or application event dispatch. The wait strategy times out after 60 seconds looking for `Transitioning from RECOVERY to RUNNING`. Captured broker output reaches transaction-coordinator startup but does not contain the required running transition. Container logs also report a CDS archive mismatch for `jdk.management.agent`; its relevance is not yet established. The same timeout was observed during TASK-301 and earlier TASK-303 verification, so it is not established as a generic-event regression.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 Reproduce the Kafka startup failure independently of generic event declarations and identify the actual broker-side cause.
- [ ] #2 Apply a supported, documented fix to the test infrastructure or environment; preserve failure reporting and genuine readiness verification.
- [ ] #3 Verify the Avro and Kafka example integration suites with the configured broker and Schema Registry versions.
- [ ] #4 Complete the full Maven reactor and record results, including any separate container-image download or Pub/Sub emulator blockers.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- TASK-303 implementation and focused/full affected-module reactors are green, but its required full-root verification remains blocked by this failure.
- The same final run reports 32 Kafka-example application-context errors from the Kafka readiness failure. Pub/Sub then repeatedly pulls the emulator image without reaching startup; the owned Maven command was stopped after these repeated pulls. Treat image provisioning as a separate infrastructure symptom, not proof of an application regression.
- Final root command: `mvn -q -fae test`. Diagnostic artifact: `task-303-root-final.log` in the originating Copilot session; the Maven report is `examples/avro/target/surefire-reports/be.appify.prefab.example.avro.sale.SaleIntegrationTest.txt`.
- Do not change unrelated running containers or shared Podman configuration without checking ownership and obtaining any necessary approval.
<!-- SECTION:NOTES:END -->
