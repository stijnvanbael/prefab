---
id: TASK-336
title: Set up Dependabot with automatic dependency update PRs
status: Done
assignee: []
created_date: '2026-11-18 12:24'
updated_date: '2026-11-18 18:02'
labels:
  - devops
  - dependencies
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Configure Dependabot to create dependency update pull requests automatically for the Maven monorepo and GitHub Actions workflows. Set an appropriate update cadence and PR grouping or limits for maintainable reviews. This task covers creating PRs, not automatically merging them.
<!-- SECTION:DESCRIPTION:END -->

## Analysis

Dependabot supports listing multiple manifest directories in a single Maven update configuration. This repository has a root Maven project, nested module and example projects, plus one Maven prototype under `backlog/prototypes`; all are included so the configuration covers every current `pom.xml`.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Add a valid `.github/dependabot.yml` configuration for Maven dependency updates across the root project and nested Maven projects, and for GitHub Actions updates.
- [x] #2 Configure a regular update cadence and sensible pull request limits or grouping so updates are proposed automatically without flooding the repository.
- [x] #3 Verify the configuration is valid and that the supported dependency manifests and action references are covered.
- [x] #4 Document any required repository settings for Dependabot to create update pull requests; do not enable automatic merging.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Configure weekly Maven and GitHub Actions updates. Group Maven minor/patch updates by dependency across project directories, leave major updates individually reviewable, and cap open PRs. Validate manifest and action coverage and document the repository settings for Dependabot.
<!-- SECTION:PLAN:END -->

## Implementation Notes

- Added weekly Monday checks for Maven and GitHub Actions, with caps of 10 and 5 open PRs respectively. Maven minor/patch updates are grouped by dependency across directories; major updates remain individually reviewable.
- Configured all 23 current Maven manifest directories, including the prototype, and both workflow files are covered by the GitHub Actions ecosystem.
- Dependabot version updates are enabled by committing this configuration to the default branch. If version updates have been explicitly disabled in repository settings, re-enable them under **Settings > Advanced Security > Dependabot version updates**. No auto-merge setting is enabled.
- Validated YAML syntax, update schedules, and exact Maven manifest coverage.
