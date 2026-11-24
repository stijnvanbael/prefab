---
id: TASK-344
title: Compact agent cheat sheet for developing Prefab applications
status: Done
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
- [x] #1 A cheat sheet for Prefab app development exists and is at most about 3,000 tokens
- [x] #2 It explains the mental model: what the developer writes (annotated domain records, handlers, mixins) versus what Prefab generates (controllers, services, repositories, request/response records, consumers, migrations, test helpers)
- [x] #3 It contains a lookup table from common intents (for example 'expose a filtered list', 'react to an event', 'add a custom query') to the annotations or types to use
- [x] #4 It contains an explicit 'do not' list that counters plain Spring Boot habits, including hand-written controllers, JPA entities, hand-written migrations and editing target/generated-sources
- [x] #5 It states when to stop: which documented extension points to use when the model can't express a requirement, and when to escalate to a human or stronger model instead of working around generated code
- [x] #6 It points to where deeper, task-specific guidance lives, without inlining it
- [x] #7 Deprecated APIs and alternative ways of doing the same thing are left out, so there is one recommended way per intent
- [x] #8 Benchmark results from TASK-343 are recorded for Haiku with the cheat sheet compared to without it
<!-- AC:END -->

## Notes

- Reference the per-project agent context file (`target/prefab/agent-context.md`, see TASK-345 and `generated-artefacts.md` §6.12) from the cheat sheet.

TASK-347 note: the cheat sheet's "do not" list should mention that Prefab reports hand-written controllers, JPA
entities, repositories and migrations for aggregates at compile time (`generated-artefacts.md` §6.13), and that
`@SuppressWarnings("prefab.handwritten")` / `-- prefab:manual` are for human decisions only.

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Cheat sheet: `backlog/docs/agent-cheat-sheet.md` (about 7.8 KB, roughly 2k tokens). Linked from the developer guide index.
- Added benchmark context `benchmark/contexts/cheat-sheet` (cheat sheet only in CLAUDE.md) and documented it in `benchmark.md`.
- AC #8: Haiku 5.5 with the cheat sheet passed 24/32 against 21/32 without docs, with 16 vs 25 median turns, 1m19s vs 2m00s and $0.60 vs $1.08. Recorded in `benchmark.md` under "Recorded baselines". Run in a container without Docker, so Docker-dependent tasks fail in both setups and the pass rates are not comparable with earlier baselines. A repeat on a Docker machine with `--repeat 3` is still worthwhile.
- Deprecated APIs (`PublishesEvents.publish`, `@MongoMigration`, `ignoreCase`) are deliberately left out, except the one 'do not' line for event publishing.
<!-- SECTION:NOTES:END -->
