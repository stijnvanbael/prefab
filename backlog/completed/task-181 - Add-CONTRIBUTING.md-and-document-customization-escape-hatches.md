---
id: TASK-181
title: Add CONTRIBUTING.md and document customization escape hatches
status: Done
assignee: []
created_date: '2026-05-08 16:38'
updated_date: '2026-05-22 18:06'
labels: []
dependencies: []
priority: medium
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a `CONTRIBUTING.md`, document the escape-hatch pattern (copy generated file to `src/` to prevent regeneration), and promote `@RepositoryMixin` as a first-class extension point in the Developer Guide. Lowers the onboarding bar for contributors and clarifies customization options for application developers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 CONTRIBUTING.md exists at the repository root covering project structure, how to run tests, and how to write a plugin
- [x] #2 The escape-hatch pattern (copy generated file to src/ to prevent regeneration) is documented in the Developer Guide
- [x] #3 Each @RepositoryMixin usage is documented as a first-class extension point with examples alongside @Filter usage
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Documented escape-hatch pattern (source-file override) in generated-artefacts.md §6.11 with the exact compiler NOTE message, a decision table, and a revert instruction. Cross-referenced from extension-points.md §8.6. @RepositoryMixin in §8.2 expanded into a self-contained section with derived-query and @Query examples, plus a @RepositoryMixin-vs-@Filter decision table.

Added CONTRIBUTING.md at the repository root covering: project structure (module layout + key source directories in annotation-processor), how to build and run unit vs. integration tests with code examples, a four-step guide to writing a PrefabPlugin with a full worked example and callback reference table, coding standards summary (links to AGENTS.md), and PR submission workflow with backlog task reference rule.
<!-- SECTION:NOTES:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
