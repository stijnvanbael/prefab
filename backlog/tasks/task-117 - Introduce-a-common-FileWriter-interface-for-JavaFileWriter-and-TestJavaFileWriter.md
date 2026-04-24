---
id: TASK-117
title: >-
  Introduce a common FileWriter interface for JavaFileWriter and
  TestJavaFileWriter
status: To Do
assignee: []
created_date: '2026-04-10 05:00'
updated_date: '2026-04-24 06:57'
labels:
  - "\U0001F527refactor"
dependencies: []
ordinal: 138000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`JavaFileWriter` and `TestJavaFileWriter` both serve the same conceptual purpose – writing a generated `TypeSpec` as a Java source file – but they use entirely different mechanisms:

- `JavaFileWriter` uses `ProcessingEnvironment.getFiler()` (standard annotation-processor API) to write to the main source output directory.
- `TestJavaFileWriter` bypasses the Filer API and instead uses `ToolProvider.getSystemJavaCompiler()` to write directly to a `target/prefab-test-sources/` directory on disk.

Callers (`HttpWriter`, `ApplicationWriter`, `PersistenceWriter`, `TestClientWriter`, and the various plugin writers) must know which concrete class to instantiate, which couples them to the file-writing mechanism.

The refactoring introduces a shared `FileWriter` interface (or abstract class) with a single `writeFile(packagePrefix, typeName, typeSpec)` method. `JavaFileWriter` and `TestJavaFileWriter` become the two implementations. Writers and plugins that need to write files accept a `FileWriter` (interface type) rather than a concrete class, making it straightforward to swap or mock the implementation in tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A FileWriter interface (or abstract class) is introduced with a writeFile(String packagePrefix, String typeName, TypeSpec type) method
- [ ] #2 JavaFileWriter and TestJavaFileWriter both implement the interface without changing their existing behavior
- [ ] #3 All writer classes (HttpWriter, ApplicationWriter, PersistenceWriter, TestClientWriter, plugin writers) depend on the FileWriter interface rather than the concrete classes where appropriate
- [ ] #4 All existing annotation-processor tests continue to pass after the refactoring
<!-- AC:END -->
