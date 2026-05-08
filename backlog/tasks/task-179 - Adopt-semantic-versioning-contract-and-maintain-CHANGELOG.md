---
id: task-179
title: Adopt semantic versioning contract and maintain CHANGELOG
status: To Do
assignee: []
created_date: '2026-05-08 16:38'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Adopt a formal semantic versioning contract with a `CHANGELOG.md`. Breaking annotation-level or behavior changes must be preceded by at least one minor-version deprecation cycle. This gives users a reliable upgrade path and signals framework maturity.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 CHANGELOG.md is added to the repository root
- [ ] #2 All future releases include a changelog entry distinguishing breaking changes, new features, and bug fixes
- [ ] #3 Breaking annotation or behavior changes are preceded by a deprecation cycle of at least one minor version
- [ ] #4 The release process (release.sh) is updated to require a changelog entry before tagging
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
