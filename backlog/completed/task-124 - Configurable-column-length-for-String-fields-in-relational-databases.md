---
id: TASK-124
title: Configurable column length for String fields in relational databases
status: Done
assignee:
  - '@copilot'
created_date: '2026-04-18 05:53'
updated_date: '2026-04-30 06:04'
labels:
  - postgres
  - annotation-processor
dependencies: []
priority: medium
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Strings are implicitly mapped to VARCHAR(255) in relational databases (e.g. PostgreSQL via Spring Data JDBC). When a value stored in the DB exceeds 255 characters an unexpected DataAccessException is thrown at runtime with no compile-time or startup warning. Prefab already honours @Size(max=N) to generate VARCHAR(N) columns, so developers can control the column length via the standard Jakarta Validation annotation. What is still missing is a way to map a String field to TEXT (unlimited length) for free-text content, a compile-time warning when no explicit size constraint is present, and documentation that explains the VARCHAR(255) default and the available options.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A dedicated @Text / @Lob annotation (or equivalent) maps a String field to TEXT (unlimited) in the generated migration
- [x] #2 Annotation processor emits a compile-time warning when a String field has no explicit length or @Text annotation, guiding developers toward an explicit choice
- [x] #3 Generated Flyway migration scripts honour the configured column length / TEXT type
- [x] #4 Documentation is updated to describe the available options and the default VARCHAR(255) limitation
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add @Text annotation to core module
2. Add TEXT to DataType.Primitive enum
3. Update DataType.typeOf() to return TEXT when @Text is present
4. Add helper methods isTextAnnotated() and hasSizeConstraint() to DataType
5. Emit compile-time WARNING in DbMigrationWriter for unconstrained String fields (skip @Id fields)
6. Add test source files for @Text and unconstrained String scenarios
7. Add tests to DbMigrationWriterTest
8. Update readme.md documentation
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Added `@Text` annotation to `prefab-core` (`be.appify.prefab.core.annotations.Text`)
- Added `TEXT` to `DataType.Primitive` enum and updated `DataType.parse()` to recognise it
- Updated `DataType.typeOf()` to return `TEXT` when `@Text` is present on a `String` field
- Added `DataType.isTextAnnotated()` and `DataType.hasSizeConstraint()` helpers
- `DbMigrationWriter.warnIfUnconstrainedString()` emits a `WARNING` diagnostic when a `String` field has neither `@Size` nor `@Text` (skips `@Id` fields)
- Added test sources: `dbmigration/textcolumn/source/Article.java` and `dbmigration/unconstrainedstring/source/Note.java`
- Added 5 new tests to `DbMigrationWriterTest`
- Updated `readme.md` under the Audit trail section to document the VARCHAR(255) default and the `@Size`/`@Text` options
<!-- SECTION:NOTES:END -->
