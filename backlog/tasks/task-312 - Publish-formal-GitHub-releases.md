---
id: TASK-312
title: Publish formal GitHub releases alongside Maven Central releases
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [release, devops]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Extend the existing release workflow so users receive a formal GitHub Release for a successfully published Prefab version, not only a Git tag and Maven deployment.

Related: completed TASK-041 (Maven publication) and TASK-179 (semantic versioning/changelog). `.github/workflows/release.yml` invokes `release.sh`, which tags and deploys but does not create a GitHub Release. Preserve the repository's existing tag convention.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A release is created against the exact release tag/commit only after artifact publication is confirmed; failed publication never produces a success-shaped public release.
- [ ] #2 Notes identify version, changes, breaking changes/upgrade guidance when applicable, Maven coordinates and documentation links; use TASK-179's changelog when available.
- [ ] #3 Reruns are idempotent for an existing tag/release and support recovery after partial failure without republishing or retagging artifacts.
- [ ] #4 Stable versus prerelease classification is explicit, permissions are minimal, and release inputs are validated.
- [ ] #5 Document and verify successful, failed-publication and rerun paths using existing workflow/script test conventions.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Map release phases and publication confirmation, add an idempotent GitHub Release step using the existing automation, and document recovery before enabling it.
<!-- SECTION:PLAN:END -->
