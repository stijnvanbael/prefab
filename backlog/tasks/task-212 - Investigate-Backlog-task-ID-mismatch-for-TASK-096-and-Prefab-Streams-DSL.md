---
id: TASK-212
title: Investigate Backlog task ID mismatch for TASK-096 and Prefab Streams DSL
status: Done
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-21 06:21'
labels:
  - backlog
  - tooling
  - follow-up
dependencies: []
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - >-
    backlog/tasks/task-096 -
    Generate-Open-API-documentation-for-REST-endpoints.md
priority: medium
ordinal: 25200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`task_search` returns `TASK-096 - Prefab Streams DSL`, while `task_view TASK-096` resolves to `Generate Open API documentation for REST endpoints`. Investigate duplicate/conflicting IDs and repair backlog metadata/index consistency.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Root cause of inconsistent `TASK-096` resolution is identified and documented
- [x] #2 Backlog task metadata is corrected so `task_search` and `task_view` resolve the same task for each ID
- [x] #3 No duplicate task IDs remain in active backlog tasks
- [x] #4 A regression check confirms CLI task lookup consistency after the fix
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Investigating duplicate frontmatter ID mapping: `task-099 - Prefab-Streams-DSL.md` currently declares `id: TASK-096`, conflicting with `task-096 - Generate-Open-API-documentation-for-REST-endpoints.md`.

Planned fix: update `task-099` frontmatter ID to `TASK-099`, then verify `task_view TASK-096` and `task_view TASK-099` resolve correctly and no duplicate IDs remain.

Applied direct metadata fix in `backlog/tasks/task-099 - Prefab-Streams-DSL.md`: `id: TASK-096` -> `id: TASK-099` (MCP does not expose task ID editing).

Validation results: `task_view TASK-096` resolves OpenAPI task, `task_view TASK-099` resolves Prefab Streams DSL, grep confirms only one `id: TASK-096` and one `id: TASK-099` in active task files.
<!-- SECTION:NOTES:END -->
