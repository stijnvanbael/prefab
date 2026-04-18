---
id: TASK-127
title: MCP (Model Context Protocol) support and optional REST module
status: To Do
assignee: []
created_date: '2026-04-18 09:07'
updated_date: '2026-04-18 09:13'
labels:
  - mcp
  - feature
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Extend Prefab so that both REST and MCP are optional, pluggable exposure strategies rather than REST being hardwired. A new prefab-rest module extracts the existing REST controller generation, and a new prefab-mcp module adds MCP tool generation backed by Spring AI MCP server. When exactly one strategy module is on the classpath all endpoints default to that strategy automatically, preserving full backwards compatibility. When both modules are present the developer must be explicit via an endpoints attribute on each annotation, e.g. @Create(endpoints = {Endpoint.REST, Endpoint.MCP}). The same service layer is reused by all strategies so business logic and validation are never duplicated.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new optional Maven module prefab-rest is introduced; it contains all generated REST controller code extracted from the current core generation
- [ ] #2 A new optional Maven module prefab-mcp is introduced; it generates MCP tool classes backed by the Spring AI MCP server auto-configuration
- [ ] #3 Existing projects that only have prefab-rest on the classpath behave identically to today; no code changes are required (full backwards compatibility)
- [ ] #4 When only prefab-mcp is on the classpath all annotated endpoints default to MCP exposure only, without any additional annotation changes
- [ ] #5 When both prefab-rest and prefab-mcp are on the classpath, the annotation processor emits a compile error if endpoints is not specified on an annotated constructor or method
- [ ] #6 An Endpoint enum with values REST and MCP is added to prefab-core; @Create, @GetById, @GetList, @Update, and @Delete each gain an endpoints attribute of type Endpoint[]
- [ ] #7 Generated REST controllers and MCP tool classes both delegate to the same generated service layer; business logic and validation are not duplicated
- [ ] #8 The readme is updated: prefab-rest is documented as the REST module, prefab-mcp as the MCP module, the endpoints attribute is explained with examples for single-strategy and dual-strategy setups
- [ ] #9 MCP tool names and descriptions are derived from the aggregate class name and annotation type; a new @Doc annotation (usable on constructors, methods, and classes) allows developers to override the human-readable name and description for any exposure strategy
<!-- AC:END -->
