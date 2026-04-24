---
id: TASK-128
title: >-
  parseSql crashes with ArrayIndexOutOfBoundsException on DROP CONSTRAINT
  statements
status: Done
assignee: []
created_date: '2026-04-18 11:09'
updated_date: '2026-04-22 13:38'
labels:
  - bug
dependencies: []
priority: high
ordinal: 6000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When reading existing generated migrations that contain ALTER TABLE ... DROP CONSTRAINT ... statements, DbMigrationWriter.parseSql throws a RuntimeException wrapping an ArrayIndexOutOfBoundsException (Index -1 out of bounds for length 0) from within the jsqlparser CCJSqlParser tokenizer. This blocks compilation for any project that has such a migration.

Reproduction steps:
1. Have a project with a Prefab 0.4.x-generated V2__generated.sql containing DROP CONSTRAINT statements (e.g. produced when FK index generation was introduced)
2. Upgrade the parent POM to prefab-parent 0.5.0
3. Run mvn compile

Expected: compilation succeeds (or at least fails with a meaningful Prefab error)
Actual: Fatal error compiling - ArrayIndexOutOfBoundsException inside jsqlparser CCJSqlParserTokenManager

Full stack trace:
  be.appify.prefab.processor.dbmigration.DbMigrationWriter.parseSql (DbMigrationWriter.java:210)
  net.sf.jsqlparser.parser.SimpleCharStream.getBeginLine (SimpleCharStream.java:324)
  net.sf.jsqlparser.parser.CCJSqlParserTokenManager.jjFillToken
  net.sf.jsqlparser.parser.CCJSqlParserTokenManager.getNextToken

Example offending SQL:
  ALTER TABLE chapter DROP CONSTRAINT chapter_course_fk;

Note: in 0.4.0-SNAPSHOT the same SQL was parsed successfully because parseSql short-circuited on an earlier validation error before reaching this statement, or the new 0.5.0 parseSql forEach execution path exposes the crash differently.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 parseSql handles DROP CONSTRAINT statements without crashing
- [ ] #2 Projects with V2-style DROP CONSTRAINT migrations compile successfully after upgrading to 0.5.0
- [ ] #3 Stack trace no longer contains: net.sf.jsqlparser.parser.SimpleCharStream.getBeginLine with Index -1 out of bounds
<!-- AC:END -->
