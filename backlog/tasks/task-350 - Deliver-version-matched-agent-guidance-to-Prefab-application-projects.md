---
id: TASK-350
title: Deliver version-matched agent guidance to Prefab application projects
status: To Do
assignee: []
created_date: '2026-11-20 20:14'
labels:
  - agents
  - documentation
  - dx
dependencies:
  - TASK-344
  - TASK-346
priority: high
type: feature
ordinal: 195000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
All Prefab documentation lives in the framework repository under backlog/docs. An AI agent working in a user's application never sees it; the application only depends on the published jars. Even agent material written specifically for Haiku (cheat sheet TASK-344, recipes TASK-346) is useless unless it reaches the user's project.

It must also match the Prefab version the application uses. Docs on main can describe features that aren't released yet, or drop ones the user still relies on, and a model can't tell the difference.

This task gets the agent guidance into application projects with minimal setup, in a form that common coding agents (Claude Code, Copilot, Cursor and similar) pick up automatically, and that loads detailed recipes only when needed rather than all at once.

Related: TASK-125 (how agents discover Prefab), TASK-314 (public docs site, a candidate host for an llms.txt-style entry point) and TASK-345 (per-project generated context, which this guidance should point to).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A developer can add the Prefab agent guidance to a new or existing application project with a single documented step
- [ ] #2 The guidance delivered matches the Prefab version the project depends on, and updating Prefab gives a documented way to update the guidance
- [ ] #3 Claude Code loads the cheat sheet automatically and loads recipes only when relevant; at least one other common coding agent is supported or its setup is documented
- [ ] #4 Project-specific instructions the developer has written are never overwritten
- [ ] #5 The delivered guidance points agents to the generated per-project context file (TASK-345)
- [ ] #6 Setup is documented in the readme and getting-started guide, and the TASK-343 benchmark uses this same delivery path
<!-- AC:END -->
