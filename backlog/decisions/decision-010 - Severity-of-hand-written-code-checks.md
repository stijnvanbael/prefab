---
id: decision-010
title: Severity of hand-written code checks
date: '2026-11-23'
status: accepted
related_tasks:
  - TASK-347
  - TASK-344
---

## Context

AI models and new users often hand-write controllers, JPA entities, repositories and Flyway scripts for aggregates
that Prefab already covers. TASK-347 adds compile-time checks for this. The checks must give agents a signal that is
hard to ignore, without breaking adopters whose code legitimately sits next to Prefab (legacy classes, copied
generated migrations).

## Decision

- Findings about Java code (controller clashes, entities, repositories) are compiler **errors**. A warning is easily
  ignored by an agent, and a duplicate endpoint or entity is a real defect.
- Findings about SQL migrations are **warnings**. The documented flow copies a generated script into
  `src/main/resources/db/migration` under a unique name, and a copy is indistinguishable from a hand-written script,
  so an error would break existing builds.
- `-Aprefab.handwritten.severity=error|warning` selects one severity for every finding; the standard plugin switch
  `-Aprefab.plugin.handwrittencode.enabled=false` turns the checks off.
- Opt-outs are explicit and greppable: `@SuppressWarnings("prefab.handwritten")`, a migration named `*generated`,
  `*manual` or `*baseline`, or the comment `-- prefab:manual`. The source-file override (§6.11) and
  `@RepositoryMixin` are never reported.

## Consequences

Agents see failing builds for duplicated Java code and must follow the `Suggested fix`. Existing applications can
adopt the release by setting the severity to `warning` for one release. The matching is name- and path-based, so a
same-named entity in an unrelated bounded context needs `@SuppressWarnings("prefab.handwritten")`.
