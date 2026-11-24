---
id: TASK-378
title: Ship OpenRewrite recipes to upgrade Prefab applications
status: To Do
assignee: []
created_date: '2026-11-24 10:00'
labels:
  - dx
  - agents
  - productivity
dependencies:
  - TASK-179
priority: medium
type: feature
ordinal: 208000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab is pre-1.0 and its API still moves: deprecations such as the mutable `@Update void` pattern (TASK-176),
renamed attributes, annotations moving packages. Every application then has to be migrated by hand, and an agent
migrating it works from a changelog it may not have, against docs for a version it may not use (TASK-350). Upgrades
that take an afternoon get postponed, and stale applications miss fixes.

OpenRewrite recipes make an upgrade one command (`mvn rewrite:run` with Prefab's recipe), deterministic and
reviewable, and they double as a precise description of each breaking change. They fit the semantic-versioning
contract (TASK-179) and the stable API surface (TASK-367).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `prefab-rewrite` module publishes a recipe per release that upgrades applications from the previous minor version (dependency versions plus code changes)
- [ ] #2 Every deprecation or breaking change in the stable API (TASK-367) comes with a recipe or a documented reason why it cannot be automated; the release checklist enforces this
- [ ] #3 Recipes are tested with OpenRewrite's before/after tests, and the showcase compiled against the previous release upgrades cleanly in CI
- [ ] #4 The CHANGELOG (TASK-179) and the agent guidance tell users and agents to run the recipe first
<!-- AC:END -->
