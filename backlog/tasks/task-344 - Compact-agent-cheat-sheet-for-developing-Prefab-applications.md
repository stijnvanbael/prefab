---
id: TASK-344
title: Compact agent cheat sheet for developing Prefab applications
status: To Do
assignee: []
created_date: '2026-11-20 20:13'
labels:
  - agents
  - documentation
  - dx
dependencies:
  - TASK-343
priority: high
type: docs
ordinal: 189000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
An AI agent working on a Prefab application currently has nothing compact to orient itself. The developer guide in backlog/docs is reference prose of about 55k tokens; annotation-reference.md alone is about 81 KB. The repo's AGENTS.md only points to .github/copilot-instructions.md, which covers generic Java style and the backlog workflow and says nothing about using Prefab. Small models like Claude Haiku lose focus in large reference docs.

Without guidance, models fall back on their Spring Boot training: they hand-write @RestController classes, JPA @Entity classes, Lombok, repositories and Flyway migrations that Prefab already generates. When Prefab can't express a requirement, they hack around the generated code instead of using a documented extension point or handing the task to a stronger model.

This task delivers the always-loaded entry point for app developers (not framework contributors): a short, model-oriented cheat sheet meant to sit in a user project's AGENTS.md/CLAUDE.md. Task recipes (separate task) and per-project generated context build on it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A cheat sheet for Prefab app development exists and is at most about 3,000 tokens
- [ ] #2 It explains the mental model: what the developer writes (annotated domain records, handlers, mixins) versus what Prefab generates (controllers, services, repositories, request/response records, consumers, migrations, test helpers)
- [ ] #3 It contains a lookup table from common intents (for example 'expose a filtered list', 'react to an event', 'add a custom query') to the annotations or types to use
- [ ] #4 It contains an explicit 'do not' list that counters plain Spring Boot habits, including hand-written controllers, JPA entities, hand-written migrations and editing target/generated-sources
- [ ] #5 It states when to stop: which documented extension points to use when the model can't express a requirement, and when to escalate to a human or stronger model instead of working around generated code
- [ ] #6 It points to where deeper, task-specific guidance lives, without inlining it
- [ ] #7 Deprecated APIs and alternative ways of doing the same thing are left out, so there is one recommended way per intent
- [ ] #8 Benchmark results from TASK-343 are recorded for Haiku with the cheat sheet compared to without it
<!-- AC:END -->
