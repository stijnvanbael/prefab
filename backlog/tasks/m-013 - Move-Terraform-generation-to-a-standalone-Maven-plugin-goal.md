---
id: M-013
title: Move Terraform generation to a standalone Maven plugin goal
status: To Do
assignee: []
created_date: '2026-05-08 16:38'
labels: []
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Move Terraform generation out of the annotation processor (which runs at `mvn compile`) into a dedicated `prefab-terraform-maven-plugin` with an explicit `mvn prefab:terraform` goal. Infrastructure teams can invoke it on demand; application developers are no longer affected by it during normal builds.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Terraform generation is removed from the annotation processor compile phase
- [ ] #2 A prefab-terraform-maven-plugin is created with an explicit 'terraform' goal
- [ ] #3 The goal is not bound to any default lifecycle phase
- [ ] #4 Developer Guide updated to remove Terraform from the compile-time artefacts section and add it to an Infrastructure section
- [ ] #5 Existing Terraform generation behaviour is preserved for users who invoke the plugin goal
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
