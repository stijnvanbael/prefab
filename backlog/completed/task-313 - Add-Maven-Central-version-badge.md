---
id: TASK-313
title: Add a Maven Central version badge to the README
status: Done
assignee: []
created_date: '2026-11-16 15:43'
updated_date: '2026-11-18 12:04'
labels:
  - documentation
  - release
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Show the latest published stable Prefab version near the README title with a Maven Central badge linked to the canonical artifact listing. This advertises published artifacts, not the current development POM or an unpublished Git tag.

Related: TASK-308 (README) and completed TASK-041 (Maven publication). Keep this a mechanical documentation change.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The badge uses the actual published `be.appify.prefab:prefab-core` coordinates and links to their Maven Central listing.
- [x] #2 Version updates are automatic after publication; no hard-coded release version or snapshot badge is introduced.
- [x] #3 The badge has meaningful alternative text, renders on GitHub, and its target is verified.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Use the published prefab-core coordinates for a dynamic Maven Central badge directly below the README title; check the badge endpoint and canonical artifact page before closing.

Added a linked Shields.io Maven Central badge directly below the README title. Verified the Sonatype listing and confirmed that the badge SVG matches the release in Maven Central metadata. GitHub Markdown rendering produced the expected link, image and alt text.
<!-- SECTION:NOTES:END -->
