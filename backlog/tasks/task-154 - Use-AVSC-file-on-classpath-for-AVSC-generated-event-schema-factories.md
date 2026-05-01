---
id: TASK-154
title: Use AVSC file on classpath for AVSC-generated event schema factories
status: Done
assignee: []
created_date: '2026-05-01 05:02'
updated_date: '2026-05-01 05:16'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
For AVSC-generated events, the Avro schema factory currently reconstructs the schema by walking the object tree at startup (Schema.createRecord, Schema.createField, etc.). This is error-prone and can drift from the original AVSC file. Instead, the generated schema factory for AVSC-backed event records should load and parse the AVSC file directly from the classpath using new Schema.Parser().parse(stream). The AVSC file is the single source of truth, so the generated schema factory should use it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 EventSchemaFactoryWriter detects AVSC-generated records and generates classpath-loading schema factories
- [x] #2 Generated schema factory uses Schema.Parser().parse(stream) to load the AVSC file from the classpath
- [x] #3 Multi-path AVSC interfaces correctly map each record to its corresponding AVSC file
- [x] #4 Existing tests pass and new tests verify the classpath-loading schema factory content
<!-- AC:END -->
