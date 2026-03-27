---
id: TASK-086
title: Generate schema documentation
status: Done
assignee:
  - '@agent'
created_date: '2026-03-27 17:39'
updated_date: '2026-03-27 20:42'
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
- [x] #1 Add @Tag annotation to generated controller classes
- [x] #2 Add @Operation annotation to each generated controller method (create, getById, getList, update, delete, download)
- [x] #3 @ApiResponse annotations are added to each controller method with appropriate response codes
- [x] #4 Documentation annotations are only generated when springdoc-openapi is on the classpath
- [x] #5 AsyncAPI 2.6.0 JSON file generated in META-INF/async-api/asyncapi.json
- [x] #6 Each @Event type documented as a channel with its topic
- [x] #7 Event fields documented as JSON Schema properties
- [x] #8 Sealed interface events documented with oneOf across permitted subtypes
- [x] #9 Content type is application/avro for AVRO events, application/json for JSON
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
Generated AsyncAPI 2.6.0 documentation for all @Event annotated types.

- Added `EventSchemaDocumentationPlugin` (PrefabPlugin) that collects all @Event types
- Added `EventSchemaDocumentationWriter` that builds an AsyncAPI 2.6.0 JSON document
- Output: `META-INF/async-api/asyncapi.json` in class output directory
- Each event topic becomes a channel with a publish operation
- Sealed interface events use `oneOf` across permitted subtypes
- AVRO events use `contentType: application/avro`, JSON events use `application/json`
- Fields mapped to JSON Schema types (string, integer, number, boolean, array, $ref)
- Object-type fields nullable by default (unless primitive or @NotNull)
- Registered in META-INF/services as a PrefabPlugin
- 3 new tests added: simpleJsonEvent, sealedInterfaceEvent, avroEvent
- All 22 tests pass
<!-- SECTION:NOTES:END -->
