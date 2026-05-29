<CRITICAL_INSTRUCTION>

**IMPORTANT: Make sure the USER PROMPT and THESE CRITICAL INSTRUCTIONS are visible to you at all times during this session. Re-read them after any interruption.**

## BACKLOG WORKFLOW INSTRUCTIONS

This project uses **Backlog.md**, you can access it with the following MCP toolset **`backlog-prefab`**.

Only edit backlog markdown files directly when the MCP tools are unavailable — otherwise use the tools to keep metadata and history consistent.

### Per-turn protocol

1. **Before starting any work**: view the relevant task (`task_view` or
   read `backlog/tasks/**.md`) — check acceptance criteria and current status.
2. **Before creating a new task**: search first (`task_search` or scan
   `backlog/tasks/`) to avoid duplicates.
3. **After each meaningful code change**: commit with a conventional-commits message
   (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`).
4. **After completing a task**: set status to `Done` and add implementation notes — do
   this before moving on to the next task.
5. **When a plan changes mid-task** (user interrupts, pivots, or uncovers a side-issue):
  - If the side-issue is out of scope → create a new task for it rather than silently
    doing the work.
  - Re-read *these* instructions before resuming so the workflow is fresh.

### When to create a task

Create a task when the work requires planning or decision-making ("How do I do this?").
Skip it for trivial/mechanical changes (typos, version bumps, etc.).

---

# GitHub Copilot Instructions

## Overview

This is a Java 25 Maven project. Write code like a modern senior software engineer —
clean, idiomatic, testable, and maintainable.

---

## Language & Platform

Target **Java 25**. Use modern features:
- Records, sealed classes, pattern matching in `switch`, primitive patterns
- Text blocks, `var` for local inference, `instanceof` pattern matching
- New-style accessors (`name()` not `getName()`); method references over lambdas
- Stream Gatherers (`Stream.gather()`), `SequencedCollection` / `SequencedMap`
- Virtual Threads for I/O; `StructuredTaskScope` for concurrent subtasks; `ScopedValue` over `ThreadLocal`

Avoid: raw types, `StringBuffer`, old `Date`/`Calendar`, `synchronized` (prefer `ReentrantLock`).

---

## Code Style

- **SOLID** principles — single responsibility, depend on abstractions, prefer composition.
- Small, focused methods; ≤3 parameters (use parameter objects when more are needed).
- Immutable by default — records, `final` fields, `List.copyOf()` / `Map.copyOf()`.
- `Optional` in return types for absent values from public APIs; never return `null`.
- Declarative streams over imperative loops where clarity is maintained; no side effects in pipelines.
- Checked exceptions for recoverable conditions; domain-specific unchecked for programming errors.
- Never swallow exceptions silently. Include meaningful messages and causes.
- Use `var` for local variables when the type is obvious from the right-hand side; otherwise, be explicit.
- Use imports whenever possible for readability; avoid fully qualified names in code.

---

## Architecture

- Layers: **domain model → application services → infrastructure/adapters** (Hexagonal / Ports & Adapters).
- Depend on interfaces, not concrete implementations; use dependency injection.
- Publish domain events for side effects; avoid direct service coupling.
- Enforce layer boundaries with **ArchUnit** tests.
- Consider CQRS for complex domains with diverging read/write models.

---

## Testing

- JUnit 5 + AssertJ + Mockito; Arrange-Act-Assert pattern.
- TDD preferred — write the test first.
- Descriptive `@DisplayName` or `methodUnderTest_givenCondition_expected()` names.
- `@ParameterizedTest` for multiple input scenarios.
- Testcontainers for real infrastructure (DB, Kafka); tests must be independent and idempotent.
- **Never leave a failing or flaky test.**

---

## Maven & Dependencies

- Minimal, purposeful dependencies with explicit versions.
- Maven Enforcer Plugin: ban duplicates, enforce minimum Java/Maven versions.
- `pom.xml`: dependencies first, then plugins, sorted logically.
- Look for the latest stable versions of libraries.

---

## Observability & Security

- SLF4J + Logback; `DEBUG` diagnostics, `INFO` lifecycle, `WARN` recoverable, `ERROR` failures.
- Never log PII/credentials. Structured parameters, not string concatenation.
- No hard-coded secrets — use environment variables. Parameterised queries only.
- Principle of least privilege. Audit CVEs periodically.

---

## Documentation & Git

- Javadoc on all public types/methods in shared code. Comment *why*, not *what*.
- Capture significant decisions in ADRs (`backlog/decisions/`).
- Small, focused commits; conventional-commits format (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`).
- Leave the codebase better than you found it (Boy Scout Rule).

---

## Final checklist — before saying "done"

- [ ] All tests pass reliably (run the full suite, not just the changed test)
- [ ] Zero compiler warnings; no unused imports or dead code
- [ ] No TODO/FIXME comments in committed code
- [ ] Code reads naturally without explanation
- [ ] Task file updated to `Done` with implementation notes
- [ ] Changes committed with a descriptive conventional-commit message

</CRITICAL_INSTRUCTION>