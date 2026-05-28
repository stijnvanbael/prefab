---
id: TASK-239
title: Fix MotherPlugin output target override for MAIN generation
status: Done
assignee: []
created_date: '2026-05-28 13:10'
updated_date: '2026-05-28 13:23'
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
Root cause: PrefabProcessor executed main aggregate/plugin batches without a PluginOutputScope MAIN context. Plugins relying on OutputTargetFileOutput with TEST defaults (e.g. MotherPlugin) therefore kept writing to test output.

Fix: wrapped both writeAdditionalFiles and writeGlobalFiles main-batch invocations in context.withOutputTarget(OutputTarget.MAIN, ...).

Added regression tests in GeneratePluginOverrideIntegrationTest for MotherPlugin target MAIN routing to SOURCE_OUTPUT and default routing to CLASS_OUTPUT.

Validation blocked by unrelated pre-existing compile failures in annotation-processor (EventTypeRegistrarWriter / EventSchemaDocumentationWriter).

Follow-up fix: split plugin execution batches by exact OutputTarget (DEFAULT, MAIN, TEST). DEFAULT now runs without forced scope so plugins keep their own default routing; explicit MAIN/TEST still run in scoped output targets.

Validation: `mvn -pl annotation-processor -am -Dtest=GeneratePluginOverrideIntegrationTest,TenantPluginTest,MotherPluginTest -Dsurefire.failIfNoSpecifiedTests=false test` passed (41 tests).

Validation: `mvn -pl annotation-processor -am -Dsurefire.failIfNoSpecifiedTests=false test` completed without surefire failure markers in `annotation-processor/target/surefire-reports` and `core/target/surefire-reports`.
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
<!-- SECTION:FINAL_SUMMARY:END -->
