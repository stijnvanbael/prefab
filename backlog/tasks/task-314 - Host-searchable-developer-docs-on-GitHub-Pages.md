---
id: TASK-314
title: Host searchable developer documentation on GitHub Pages
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [documentation, devops]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Publish the existing Developer Guide as a discoverable, searchable GitHub Pages site, keeping repository documentation authoritative rather than maintaining a second copy.

Related: TASK-148 (guide completeness), TASK-125 (discovery) and TASK-308 (README links). The site must work with current content; those stories are not prerequisites.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Choose and document a maintained static-site toolchain that renders existing guide Markdown, code blocks, tables and diagrams with stable URLs.
- [ ] #2 Search indexes all published guide pages and returns relevant annotation, configuration and error-message results; keyboard navigation and readable mobile layout are supported.
- [ ] #3 Pull requests build the site and check internal links; the default branch deploys through GitHub Pages with minimal permissions and no secret-bearing content.
- [ ] #4 Repository-relative links and source/example links remain correct in both GitHub Markdown and the deployed site.
- [ ] #5 Link the published site from the README and document local preview, publication ownership and current-version labeling.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Evaluate lightweight search-capable tooling, adapt rather than duplicate guide sources, validate links in a preview build, and wire Pages deployment.
<!-- SECTION:PLAN:END -->
