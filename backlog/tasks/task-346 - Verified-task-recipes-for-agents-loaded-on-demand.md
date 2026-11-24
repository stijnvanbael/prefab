---
id: TASK-346
title: 'Verified task recipes for agents, loaded on demand'
status: In Progress
assignee: []
created_date: '2026-11-20 20:14'
labels:
  - agents
  - documentation
  - dx
dependencies:
  - TASK-343
  - TASK-344
priority: high
type: docs
ordinal: 191000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Small models copy working examples far more reliably than they apply reference prose. The current docs are organised by annotation and feature (annotation-reference.md, feature-guides.md) rather than by what the developer is trying to do, and they overlap. They also contain about 130 Java snippets that are never compiled. When a model copies a snippet that has drifted out of date, it copies the mistake faithfully.

This task adds a set of short recipes, one per common intent, each built around a minimal complete example that is guaranteed to compile and work with the current version. Agents load only the recipe they need, on top of the always-loaded cheat sheet (TASK-344). The annotation reference remains the fallback for details.

Related: TASK-148 AC #5 (extract compilable snippets from examples), TASK-337 (shop showcase, a natural source of examples) and the annotation-processor fixtures (annotation-processor/src/test/resources/**/source), which are already verified minimal models.
<!-- SECTION:DESCRIPTION:END -->

## Analysis

<!-- SECTION:ANALYSIS:BEGIN -->
- The showcase contains canonical source examples and integration tests for REST CRUD, filtering/paging/sorting,
  update/domain logic, publishing and all three event-handler patterns, repository mixins, endpoint security,
  binary fields, audit fields, and generated test clients.
- Multi-tenancy has an annotation-processor fixture exercised by the processor test suite. Recipes should link to
  these verified sources instead of duplicating Java snippets that can drift.
- Add an intent index and one small recipe per requested intent. Keep each recipe under a documented token budget,
  with mistakes linked to the canonical reference section. The build enforces the 3,000-character ceiling used as
  a 750-token Markdown proxy.
- Add a benchmark context that makes the recipe files and linked examples available on demand, then record results
  against the cheat-sheet-only and full-docs contexts.
<!-- SECTION:ANALYSIS:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Added 13 intent-specific recipes and the index at `backlog/docs/recipes/`; every recipe links to canonical Java
  sources compiled by the showcase or exercised by annotation-processor tests. `RecipeDocumentationTest` enforces
  the recipe budget and validates each source link.
- Linked the recipe index from the developer guide and cheat sheet. Added a `recipes` benchmark context that copies
  the recipes and linked example sources for on-demand use; `WorkspaceFactoryTest` verifies the context.
- Validation: `mvn -pl annotation-processor,benchmark -am -Dtest=RecipeDocumentationTest,WorkspaceFactoryTest,TenantPluginTest -Dsurefire.failIfNoSpecifiedTests=false test` passes; `mvn -pl examples/showcase -am -DskipTests test` compiles the linked showcase examples.
- Attempting the linked showcase integration tests failed when Testcontainers could not pull the Schema Registry image
  (connection reset while reading the image); this is an environment/network failure, not a test assertion failure.
- Acceptance criteria #1-#5 are implemented. Criterion #6 remains blocked: the configured environment has no
  Claude Code CLI or Anthropic API key, so no benchmark runs or results were produced. Do not treat the existing
  cheat-sheet/full-docs baselines as a recipes comparison.
<!-- SECTION:NOTES:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Recipes exist for at least: aggregate with REST CRUD, filtered/paged/sorted lists, updates and domain logic, publishing events, handling events (create, update by reference, multicast), repository mixins, endpoint security, multi-tenancy, binary fields, audit fields, and testing with Prefab's generated test helpers
- [x] #2 Each recipe stays within a documented token budget and contains a minimal complete example, the key rules, and the most common mistakes with their fixes
- [x] #3 Every code example in a recipe is compiled (and where relevant tested) by the build, so a breaking API change fails the build instead of leaving an outdated recipe
- [x] #4 An index maps intents to recipes so an agent can pick the right one without reading the others
- [x] #5 Recipes use only the current recommended API; deprecated annotations and attributes do not appear
- [ ] #6 Benchmark results from TASK-343 are recorded for Haiku with cheat sheet plus recipes, compared to cheat sheet alone and to the full docs
<!-- AC:END -->
