---
id: TASK-164
title: 'Fix generated test client: do not overwrite manual override in src/test/java'
status: Done
assignee: []
created_date: ''
updated_date: '2026-05-08 05:55'
labels:
  - bug
  - annotation-processor
  - test-client
  - 'reported-by:maestro'
dependencies: []
priority: medium
---

## Problem Statement

The Prefab annotation processor unconditionally regenerates `*Client.java` test helpers
on every build. This means any manually-authored version of such a file is silently
overwritten by the generated one, even when the developer has intentionally placed a
hand-written copy in `src/test/java` to work around a code-generation defect.

### Affected scenario

A project has a manual override of Prefab's generated test client in
`src/test/java/<package>/<Aggregate>Client.java` (for example to work around the
`fix-test-client-enum-queryParam.md` bug). On the next build the processor regenerates
the file, replacing the fix and reintroducing the original bug.

Example (Maestro `ConversationSession`):

```
src/test/java/be/appify/maestro/domain/conversation/ConversationSessionClient.java
   ↑ manually maintained to call status.name() instead of passing the enum directly
```

Despite the file living in `src/test/java`, Prefab's annotation processor writes a
fresh, broken version to the generated-test-sources output directory, which takes
precedence (or clashes) during compilation.

### Observed symptom

After a clean build (or after Prefab is upgraded) the manually-crafted file is
effectively bypassed by the regenerated one, causing compile errors or incorrect
runtime behaviour that were supposed to be fixed by the override.

## Root Cause (hypothesis)

The processor writes generated test sources unconditionally to
`target/generated-test-sources/` without checking whether a corresponding source file
already exists under `src/test/java/`. The Java compiler then sees two classes with the
same fully-qualified name and either fails or picks the wrong one.

## Proposed Fix

Before emitting a generated `*Client.java`, the processor should check whether a file
with the same fully-qualified class name already exists under any source root that is
**not** the processor's own output directory (i.e. the project's actual `src/test/java`
tree).

If a manual override is found, the processor should:

1. **Skip generation** of that file and emit an `INFO`-level note via
   `processingEnv.getMessager()`:
   ```
   NOTE: Skipping generation of ConversationSessionClient — manual override found at
         src/test/java/.../ConversationSessionClient.java
   ```
2. Optionally, emit a `WARNING` if the manually-maintained file does not contain the
   expected Prefab-generated marker comment, so the developer knows the override will
   never be auto-updated.

### Alternative: honour a `@ManualOverride` / skip annotation

An annotation such as `@SuppressGeneration("ConversationSessionClient")` on the
aggregate, or a per-file `.prefab-skip` marker, would give explicit, refactor-safe
control over which files should not be regenerated.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Processor detects a manually-authored `*Client.java` in `src/test/java` and skips
      generating the corresponding file
- [ ] #2 An `INFO` diagnostic note is emitted when a file is skipped
- [ ] #3 A clean build of a project with a manual override compiles without duplicate-class
      errors
- [ ] #4 A project **without** a manual override still has the test client generated as
      before (no regression)
- [ ] #5 Unit / integration test in `prefab-annotation-processor` verifies skip behaviour
      when a source file pre-exists
- [ ] #6 Documentation updated to describe the manual-override (skip) mechanism

## Current Workaround (Maestro)

The manually-authored `ConversationSessionClient.java` is kept in `src/test/java` and
the Prefab-generated version is suppressed by adding a `<testExclude>` rule in
`maven-compiler-plugin`. This prevents the generated (broken) file from being compiled
but still requires careful maintenance whenever Prefab regenerates it.

See also: `fix-test-client-enum-queryParam.md` — the underlying bug that made this
workaround necessary in the first place.
<!-- AC:END -->
