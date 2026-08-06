---
id: TASK-273
title: Consolidate builder writers for standalone and AVSC record builders
status: Done
assignee: []
created_date: '2026-08-06 11:09'
labels:
  - annotation-processor
  - avro-processor
  - builder
  - refactor
dependencies: []
priority: medium
---

## Description

Consolidate builder generation logic used by standalone builder generation and AVSC-generated record builder generation into a shared writer abstraction.

The objective is to eliminate duplicated builder-generation behaviour, keep generated APIs consistent, and reduce maintenance overhead when evolving builder features.

## Acceptance Criteria

- [x] #1 Shared builder writer component(s) own common method/field/build generation logic currently duplicated between standalone and AVSC record builder paths
- [x] #2 Standalone builder generation continues to produce equivalent builder API/behaviour after refactor
- [x] #3 AVSC-generated record builder generation continues to produce equivalent builder API/behaviour after refactor
- [x] #4 Extension points remain clear for source-specific concerns (e.g., naming, source metadata, schema-specific details) without reintroducing duplication
- [x] #5 Regression tests cover both standalone and AVSC builder generation to prevent divergence

## Analysis

- `BuilderWriter` and AVSC-oriented generation currently solve closely related problems with overlapping output patterns.
- Divergence risk increases as builder enhancements are added (for example list adders, nullability helpers, or prefix configurability).
- A shared core writer with narrow adapters for standalone and AVSC entry points should preserve existing behaviour while centralising evolution of builder capabilities.
- `MotherWriter` currently maintains a separate standalone builder implementation (`buildStandaloneBuilderClass`) that duplicates field/setter/self/build logic from `BuilderWriter`; this is the consolidation seam.

## Implementation Notes

- Extended `BuilderWriter` with a standalone builder entry point (`standaloneBuilderClass(...)`) backed by the same internal enrichment pipeline used for nested record builders.
- Consolidated shared generation concerns (SELF type variable, fields, setters, list adders, `self()`, `build()`) into a common internal method in `BuilderWriter`.
- Refactored `MotherWriter` standalone event builder emission to delegate to `BuilderWriter` instead of maintaining a duplicate implementation.
- Added regression coverage:
  - `MotherPluginTest.standaloneEventBuilderSupportsListAdders` to validate standalone builder list-adder generation.
  - `AvscPluginTest.avscRecordBuilderSupportsListAdders` to validate AVSC nested builder list-adder generation.
