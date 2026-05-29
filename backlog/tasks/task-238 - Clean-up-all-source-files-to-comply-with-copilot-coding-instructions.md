---
id: TASK-238
title: Clean up all source files to comply with copilot coding instructions
status: To Do
assignee: []
created_date: '2026-05-27 14:18'
updated_date: '2026-05-28 05:31'
labels:
  - feature
  - messaging
  - annotation-processor
dependencies: []
priority: medium
---

## Description

Audit and clean up every Java source file across all submodules so that the code
fully complies with the project's copilot coding instructions. This includes:

- Modern Java 25 idioms (records, sealed classes, pattern matching, `var`, text blocks,
  new-style accessors, method references, Stream Gatherers, Virtual Threads where appropriate)
- SOLID principles, single responsibility, small focused methods (≤ 3 params)
- Immutability by default (`final` fields, `List.copyOf()`, `Map.copyOf()`)
- `Optional` in public APIs — never `null`
- Declarative streams over imperative loops
- Meaningful exception messages; no silent swallowing
- SLF4J structured logging — no string concatenation; no PII
- Zero compiler warnings, no unused imports, no dead code
- No `TODO`/`FIXME` comments left in committed code
- Javadoc on all public types and methods in shared code
- LF line endings (CM-001) throughout

## Acceptance Criteria

- [ ] **annotation-processor** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [x] **async-api** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **avro** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **avro-processor** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **core** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **kafka** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **mongodb** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **openapi** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **postgres** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **pubsub** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **security** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **sns-sqs** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **streams** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **terraform** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] **test** — all source files comply: modern Java idioms, no warnings,
  no dead code, no raw types, Javadoc on public API, LF line endings
- [ ] Full Maven build (`mvn verify`) passes with zero warnings and zero test failures
  after all clean-up changes are committed
- [ ] Each submodule's changes are committed separately with a `refactor:` conventional-commit message
