---
id: TASK-348
title: Actionable startup diagnostics for misconfigured Prefab applications
status: To Do
assignee: []
created_date: '2026-11-20 20:14'
labels:
  - agents
  - dx
dependencies: []
priority: medium
type: feature
ordinal: 193000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The annotation processor already gives actionable compile-time errors with a 'Suggested fix' (TASK-172, extended by TASK-323). Many failures only show up when the application starts, though: missing or wrong broker, database or tenant configuration, missing topics or subscriptions, missing provider beans. These surface as long Spring context stack traces with no Prefab-specific guidance, and troubleshooting.md has no examples for them.

AI agents fix problems quickly when the error says what is wrong and what to do; they flounder in generic stack traces. This task brings the same actionable style to startup failures.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The most common Prefab startup misconfigurations are inventoried for each runtime module (core, postgres, mongodb, kafka, pubsub, sns-sqs, security, tenancy)
- [ ] #2 Each inventoried failure produces a short report naming the cause, the missing or wrong property or bean, and the fix, instead of only a raw stack trace
- [ ] #3 The original exception stays available for debugging
- [ ] #4 Each report is covered by a test that starts a context with the misconfiguration and checks the message
- [ ] #5 troubleshooting.md lists every report with its cause and fix
<!-- AC:END -->
