---
id: TASK-086
title: Generate schema documentation
status: In Progress
assignee:
  - '@agent'
created_date: '2026-03-27 17:39'
updated_date: '2026-03-27 17:40'
labels:
  - feature
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generate OpenAPI/Swagger documentation annotations in the generated REST controllers. This includes adding @Tag to controller classes, @Operation to controller methods, and @ApiResponse annotations to describe response codes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add @Tag annotation to generated controller classes
- [ ] #2 Add @Operation annotation to each generated controller method (create, getById, getList, update, delete, download)
- [ ] #3 @ApiResponse annotations are added to each controller method with appropriate response codes
- [ ] #4 Documentation annotations are only generated when springdoc-openapi is on the classpath
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
