---
id: TASK-006
title: Skip generating classes that already exist
status: Done
assignee: []
created_date: '2025-10-10 13:32'
updated_date: '2026-04-30 06:23'
labels:
  - annotation-processor
  - code-generation
dependencies: []
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
There are four distinct scenarios in which the annotation processor encounters or attempts to generate an already-existing class. Each requires a different treatment.

### Scenario 1 — User manually writes a class that would be generated (intended override)

A developer writes their own `UserService.java`, `UserController.java`, `UserRepository.java`, or `UserResponse.java` in the same package path that the processor would generate into.

**Current behaviour:** `JavaFileWriter.writeFile()` calls `Filer.createSourceFile()`, which throws a `FilerException` when the file already exists. The exception is caught and the method silently returns — the developer's manual class is preserved.

**Problem:** This is silent. There is no feedback if the collision is accidental, and the developer does not know that their hand-written file is suppressing generation.

### Scenario 2 — Multi-round processing: `writeAdditionalFiles()` runs in every round

`PrefabProcessor.process()` calls `plugins.forEach(plugin -> plugin.writeAdditionalFiles(...))` in **every** round. Plugins such as `AvroPlugin` and `SerializationPlugin` use `context.eventElements()` and try to generate files in round 1 and again in round 2. In AVSC-based projects the round-1 file survives (correctly, because `FilerException` is caught), but the round-2 attempt is wasted work.

**Problem:** Unnecessary generation attempts and silent `FilerException` catches make it hard to reason about what was actually written and in which round.

### Scenario 3 — `TestJavaFileWriter` unconditionally overwrites

`TestJavaFileWriter.writeFile()` writes directly to `target/prefab-test-sources/` using the Java file manager. It does **not** check whether the file already exists and always opens the file for writing (creating or truncating).

**Problem:** Any manual changes made to files in `target/prefab-test-sources/` are silently lost on the next build. The current behaviour should at least be documented; an existence check can prevent accidental overwrites.

### Scenario 4 — `writeEventFiles` and `writeGlobalFiles` (already handled)

Both are guarded by boolean flags (`eventFilesWritten`, `globalFilesWritten`) in `PrefabProcessor` and are not generated more than once. No change is needed here.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 When `JavaFileWriter.writeFile()` skips a file because a source file with the same qualified name already exists (either user-written or from a prior round), a `NOTE`-level compiler message is emitted so the developer can distinguish an intentional override from an accidental collision
- [x] #2 Each plugin's `writeAdditionalFiles()` is guarded so that files it has already generated in a previous round are not attempted again (either via an explicit round-tracking set or by checking whether the target element set has changed), eliminating silent `FilerException` catches in round 2+
- [x] #3 `TestJavaFileWriter.writeFile()` checks whether the output file already exists before writing; if it does, generation is skipped and a message is printed explaining that the file in `target/prefab-test-sources/` is treated as a regenerated artifact
- [x] #4 All existing annotation-processor tests continue to pass after the changes
<!-- AC:END -->
