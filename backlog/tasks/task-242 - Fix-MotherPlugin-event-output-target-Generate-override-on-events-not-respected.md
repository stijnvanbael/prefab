---
id: TASK-242
title: >-
  Fix MotherPlugin event output target: @Generate override on events not
  respected
status: In Progress
assignee: []
created_date: '2026-05-28 15:35'
labels: []
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MotherPlugin.writeAdditionalFiles() processes event elements in every aggregate batch call, inheriting the aggregate scope. Events with @Generate(plugin=MotherPlugin.class, target=OutputTarget.MAIN) are written to test output instead of main because they inherit the DEFAULT batch scope (no output target override active) when that batch runs first. Each event element must be individually scoped to its own resolved output target.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Event mothers are written to main output when @Generate(plugin=MotherPlugin.class, target=OutputTarget.MAIN) is present on the event element
- [ ] #2 Nested record mothers for an event with OutputTarget.MAIN override are also written to main output
- [ ] #3 Events without @Generate override continue to be written to test output
- [ ] #4 Integration test validates the MAIN output routing for an @Event-annotated type
<!-- AC:END -->
