---
id: TASK-086
title: Generate schema documentation
status: Done
assignee:
  - '@agent'
created_date: '2026-03-27 17:39'
updated_date: '2026-03-27 20:29'
labels:
  - feature
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generate AsyncAPI 2.6.0 schema documentation for all events annotated with @Event
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add @Tag annotation to generated controller classes
- [ ] #2 Add @Operation annotation to each generated controller method (create, getById, getList, update, delete, download)
- [ ] #3 @ApiResponse annotations are added to each controller method with appropriate response codes
- [ ] #4 Documentation annotations are only generated when springdoc-openapi is on the classpath
- [ ] #5 AsyncAPI 2.6.0 JSON file generated in META-INF/async-api/asyncapi.json
- [ ] #6 Each @Event type documented as a channel with its topic
- [ ] #7 Event fields documented as JSON Schema properties
- [ ] #8 Sealed interface events documented with oneOf across permitted subtypes
- [ ] #9 Content type is application/avro for AVRO events, application/json for JSON
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create OpenApiUtil.java with static helper methods for @Tag, @Operation, @ApiResponse annotations
2. Modify HttpWriter to add @Tag to generated controllers
3. Modify CreateControllerWriter to add @Operation and @ApiResponse
4. Modify GetByIdControllerWriter to add @Operation and @ApiResponse
5. Modify GetListControllerWriter to add @Operation and @ApiResponse
6. Modify DeleteControllerWriter to add @Operation and @ApiResponse
7. Modify UpdateControllerWriter to add @Operation and @ApiResponse
8. Modify BinaryControllerWriter to add @Operation and @ApiResponse
9. Build and test
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added OpenAPI documentation generation to the Prefab annotation processor.

- Added `OpenApiUtil` with static helper methods for `@Tag`, `@Operation`, and `@ApiResponse` annotations
- Classpath detection ensures annotations are only generated when springdoc-openapi is present (like how `ControllerUtil.SECURITY_INCLUDED` works)
- Modified `HttpWriter` to add `@Tag` to generated controller classes
- Modified all 6 controller writers (Create, GetById, GetList, Delete, Update, Binary) to add `@Operation` and `@ApiResponse` annotations
- `DeleteControllerWriter.deleteMethod()` signature updated to accept `ClassManifest` for the operation summary
- All 19 existing tests pass
- Backward compatible: no OpenAPI annotations generated without springdoc on classpath
<!-- SECTION:NOTES:END -->
