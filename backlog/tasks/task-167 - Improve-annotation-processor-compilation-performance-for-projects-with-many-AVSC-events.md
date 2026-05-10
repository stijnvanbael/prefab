---
id: TASK-167
title: >-
  Improve annotation-processor compilation performance for projects with many
  AVSC events
status: Done
assignee:
  - '@copilot'
created_date: '2026-05-08 07:40'
updated_date: '2026-05-08 08:04'
labels:
  - performance
  - annotation-processor
  - avro
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
On large projects with many AVSC-annotated events, compilation becomes noticeably slow. Analysis of the annotation processor and avro-processor modules reveals several compounding performance bottlenecks that scale poorly with the number of AVSC files and event types.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 ServiceLoader.load() in detectPlugins() is called once per processing round; cache the plugin list across rounds instead
- [x] #2 AVSC files are parsed from disk/classpath multiple times per type in EventSchemaFactoryWriter (matchesRecordName, namedTypeFromAvsc, avroNamespaceOf); introduce a round-scoped AVSC schema cache keyed by path
- [x] #3 findAvscPath() in EventSchemaFactoryWriter iterates all @Avsc-annotated interfaces and re-parses each AVSC file for every event type lookup (O(types x avsc_files)); precompute a path-to-record-names index once per round
- [x] #4 TypeManifest, VariableManifest, and ClassManifest caches are fully cleared on every round (clearRoundCaches); stable, fully-resolved types do not need to be evicted — only ERROR-kind types should be invalidated
- [x] #5 eventElements() and eventElementsFromCurrentCompilation() in PrefabContext scan the RoundEnvironment on every call; memoize the result for the lifetime of a single round
- [x] #6 allNestedTypes() in AvroPlugin uses ArrayList.contains() for deduplication checks, resulting in O(n^2) behaviour; replace with a LinkedHashSet
- [x] #7 All existing annotation-processor and avro-processor tests continue to pass after the changes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Cache plugin list across rounds in PrefabProcessor (AC-1)
2. Memoize eventElements() in PrefabContext (AC-5)
3. Selective cache invalidation in TypeManifest/VariableManifest/ClassManifest (AC-4)
4. Round-scoped AVSC schema cache in EventSchemaFactoryWriter (AC-2)
5. Precompute path-to-record-names index in EventSchemaFactoryWriter (AC-3)
6. Replace ArrayList with LinkedHashSet in AvroPlugin.allNestedTypes() (AC-6)
7. Run tests and verify all pass (AC-7)
<!-- SECTION:PLAN:END -->
