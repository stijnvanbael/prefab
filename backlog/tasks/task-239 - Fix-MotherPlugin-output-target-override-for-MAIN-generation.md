---
id: TASK-239
title: Fix MotherPlugin output target override for MAIN generation
status: Done
assignee: []
created_date: '2026-05-28 13:10'
updated_date: '2026-05-28 14:24'
labels:
  - bug
  - mother-plugin
  - codegen
dependencies: []
references:
  - >-
    annotation-processor/src/main/java/be/appify/prefab/processor/mother/MotherPlugin.java
  - core/src/main/java/be/appify/prefab/core/annotations/Generate.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Troubleshoot why @Generate(plugin = MotherPlugin.class, target = OutputTarget.MAIN) still writes mothers to test sources and implement a fix so per-aggregate target overrides are respected.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 MotherPlugin-generated request/event mothers are emitted to main sources when target MAIN is configured.
- [x] #2 MotherPlugin continues to emit to test sources when target TEST/default behavior is selected.
- [x] #3 A regression test demonstrates target selection behavior for MotherPlugin.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented a second-stage output routing refactor to make target selection explicit and remove MAIN-routing fallback logic from test writer code paths.

Key changes:
- Added `FileOutput` as a neutral generation abstraction (instead of production code depending on `TestFileOutput`).
- Kept `TestFileOutput` as a deprecated compatibility alias extending `FileOutput` to avoid broad breakage.
- Updated `OutputTargetFileOutput` to delegate via a target->writer map and central `PluginOutputScope.effectiveTargetFor(...)` resolution.
- Removed `TestJavaFileWriter` MAIN->`JavaFileWriter` delegation; it now only writes test/class output.
- Added context matching hardening in `PluginOutputScope` so scope still applies when equivalent `PrefabContext` instances share the same `ProcessingEnvironment`.
- Migrated `MotherWriter` fields to `FileOutput` to avoid test-specific semantics in generation code.

Regression coverage:
- Added `PluginOutputScopeTest` compile-testing regression that compiles a DEFAULT aggregate and a MAIN-overridden Mother aggregate together, verifying MAIN mother output remains in source output while default stays in class output.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed MotherPlugin output-target override handling so explicit MAIN generation works without breaking plugins that default to TEST outputs.

Implementation details:
- Updated `PrefabProcessor` plugin dispatch to split aggregates/polymorphic manifests by exact output target (`DEFAULT`, `MAIN`, `TEST`) for both `writeAdditionalFiles` and `writeGlobalFiles`.
- `DEFAULT` batches now run without forced output scope (preserving each plugin's own default writer behavior).
- `MAIN` and `TEST` batches run inside explicit `context.withOutputTarget(...)` scopes.
- Existing regression tests for MotherPlugin target handling remain in place and validate MAIN/default behavior.

Validation:
- `mvn -pl annotation-processor -am -Dtest=GeneratePluginOverrideIntegrationTest,TenantPluginTest,MotherPluginTest -Dsurefire.failIfNoSpecifiedTests=false test` (pass)
- `mvn -pl annotation-processor -am -Dsurefire.failIfNoSpecifiedTests=false test` and report scan found no surefire failures in `annotation-processor/target/surefire-reports` or `core/target/surefire-reports`.

Refactored output writing around a neutral `FileOutput` abstraction and moved all target resolution into `OutputTargetFileOutput` + `PluginOutputScope` to eliminate test-writer delegation hacks.

`TestJavaFileWriter` now has a single responsibility (test/class output only), while `OutputTargetFileOutput` centrally dispatches MAIN/TEST writes.

Added `PluginOutputScopeTest` to validate mixed DEFAULT + MAIN Mother generation routing in one compilation unit.

Validation rerun: targeted routing suite (`GeneratePluginOverrideIntegrationTest`, `PluginOutputScopeTest`, `MotherPluginTest`, `TenantPluginTest`) passed; full `mvn -pl annotation-processor -am -Dsurefire.failIfNoSpecifiedTests=false test` run showed no surefire failures.
<!-- SECTION:FINAL_SUMMARY:END -->
