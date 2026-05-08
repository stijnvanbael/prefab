---
id: task-181
title: Add CONTRIBUTING.md and document customization escape hatches
status: To Do
assignee: []
created_date: '2026-05-08 16:38'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a `CONTRIBUTING.md`, document the escape-hatch pattern (copy generated file to `src/` to prevent regeneration), and promote `@RepositoryMixin` as a first-class extension point in the Developer Guide. Lowers the onboarding bar for contributors and clarifies customization options for application developers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 CONTRIBUTING.md exists at the repository root covering project structure, how to run tests, and how to write a plugin
- [ ] #2 The escape-hatch pattern (copy generated file to src/ to prevent regeneration) is documented in the Developer Guide
- [ ] #3 Each @RepositoryMixin usage is documented as a first-class extension point with examples alongside @Filter usage
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
