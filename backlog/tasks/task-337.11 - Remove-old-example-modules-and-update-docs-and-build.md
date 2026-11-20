---
id: TASK-337.11
title: Remove old example modules and update docs and build
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
labels:
  - examples
  - docs
dependencies:
  - TASK-337.08
  - TASK-337.09
  - TASK-337.10
parent_task_id: TASK-337
priority: medium
type: chore
ordinal: 182000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Once all variants are in place: delete `examples/{avro,kafka,mongodb,pubsub,sns-sqs,streams}` (and stray `pom.xml.versionsBackup` files), update `<modules>` in the root pom, `release.sh`/publishing exclusions, CI workflows, `readme.md`, the developer guide and any skill/agent docs that point at old example paths. Close TASK-153 and TASK-250 if their example code is gone.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 No references to the old example modules remain (grep the repo)
- [ ] #2 Full build including all variants passes in CI
- [ ] #3 Example modules are still excluded from Maven Central publishing
<!-- AC:END -->
