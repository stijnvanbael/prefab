---
id: TASK-145
title: Error generating migration script for aggregate roots in different packages
status: Done
assignee: [ ]
created_date: '2026-04-27 14:41'
updated_date: '2026-04-27 14:42'
labels: [ ]
dependencies: [ ]
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Fatal error when compiling a project where aggregate roots are defined in different packages.
The annotation processor attempts to reopen the same migration file, causing: java.lang.RuntimeException:
javax.annotation.processing.FilerException: Attempt to reopen a file for path
...target\classes\db\migration\V1__generated.sql
When adding the first generated file to the sources, a second generated migration will attempt to drop the table created
in the first.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [ ] #1 Annotation processor collects all aggregate roots across packages before writing the migration file
- [ ] #2 V1__generated.sql is written only once per compilation round, not once per processed aggregate root
- [ ] #3 No FilerException is thrown when aggregate roots exist in multiple packages
- [ ] #4 Generated migration script correctly reflects all aggregate roots without conflicts

<!-- AC:END -->
