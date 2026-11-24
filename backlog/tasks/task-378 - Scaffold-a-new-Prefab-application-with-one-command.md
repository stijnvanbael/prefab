---
id: TASK-378
title: Scaffold a new Prefab application with one command
status: To Do
assignee: []
created_date: '2026-11-24 10:00'
labels:
  - dx
  - agents
  - productivity
dependencies: []
priority: medium
type: feature
ordinal: 207000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`getting-started.md` starts at "add `@EnablePrefab` to your main application class". Before that, a developer or
agent has to assemble a `pom.xml` with the right Prefab modules, annotation processor configuration, Java 25 compiler
settings, test dependencies, Flyway and broker setup. That is where agents make the most unforced, hard-to-diagnose
mistakes, and it is the first thing a new user sees. The benchmark already maintains a working skeleton
(`benchmark/template`) that every task starts from, so the knowledge exists but is not shipped.

A Maven archetype (or an equivalent generator) published with each release gives a ready project in one command,
with the backend choices as parameters.

Related: TASK-024 (starter pom, which would make the generated pom smaller), TASK-350 (agent guidance, which the
scaffold should include), TASK-374 (local run, which the scaffold should configure).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 One documented command creates a project that compiles, passes its tests and starts, choosing persistence (Postgres, MongoDB) and messaging (none, Kafka, Pub/Sub, SNS/SQS) as parameters
- [ ] #2 The project contains a sample aggregate with a test, the version-matched agent guidance (TASK-350) and a CI workflow
- [ ] #3 The scaffold is published with every release at the release version, and the build generates and verifies every parameter combination so it cannot rot
- [ ] #4 `benchmark/template` is generated from the scaffold, so the benchmark measures what users get
- [ ] #5 `readme.md` and `getting-started.md` start with the command
<!-- AC:END -->
