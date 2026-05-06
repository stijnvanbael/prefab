# GitHub Copilot Instructions

When generating code in this repository, follow all guidelines defined in [AGENTS.md](../AGENTS.md).

## General Guidelines

- Keep terminal commands short. GitHub Copilot truncates long terminal commands, which can lead to incomplete or
  incorrect instructions. Instead of executing one long command, break it down into multiple shorter commands that can
  be executed sequentially. Try to edit big files with a tool instead.

---

## Overview

Prefab is a Java 21 Maven project — an annotation-driven code-generation framework for Spring Boot 4.x applications
built around the **Aggregate Root** pattern. It generates controllers, services, repositories, DTOs, event consumers,
and database migrations at compile time from annotated Java records.

Write code like a modern senior software engineer — clean, idiomatic, testable, and maintainable.

---

## Language & Platform

- Target **Java 21**. Use the latest stable language features available on this version:
    - Records, sealed classes, and pattern matching (including pattern matching in `switch`)
    - Text blocks for multi-line strings
    - `var` for local variable type inference where it improves readability
    - Prefer `instanceof` pattern matching over explicit casts
    - New style record accessors without the `get`/`set` prefix (e.g. `name()` instead of `getName()`)
    - Use method references instead of lambdas where they improve clarity
    - **`SequencedCollection`**, `SequencedSet`, `SequencedMap` for ordered collection access (Java 21+)
- Avoid deprecated APIs and legacy patterns (e.g. raw types, `StringBuffer`, old `Date`/`Calendar`).

---

## Prefab Conventions

- Annotate domain model records with `@Aggregate`. The aggregate is the single, consistent unit of data.
- Use `@Create`, `@Update`, and `@Delete` constructors/methods on aggregates to define lifecycle operations.
- Use `@GetById` and `@GetList` on the aggregate to expose read endpoints.
- Use `Reference<T>` as the ID type; initialise with `Reference.create()` in the `@Create` constructor.
- Use `@Version long version` to enable optimistic locking.
- Use `@Event` on records (or sealed interfaces) for domain events published to messaging platforms.
- Use `@EventHandler` on methods that process events to create or update aggregates.
- Use `@RepositoryMixin` interfaces to add custom query methods to generated repositories.
- Add `@EnablePrefab` to your Spring Boot application class.
- Consult the [Developer Guide](../backlog/docs/developer-guide.md) as the authoritative reference for all
  annotations, modules, generated artefacts, and configuration options.
- When adding or modifying any Prefab feature, update the Developer Guide in the same commit.

---

## Concurrency

- Prefer **Virtual Threads** (`Thread.ofVirtual()`) for I/O-bound concurrency — avoid managing thread pools manually
  for blocking tasks.
- Use **Structured Concurrency** (`StructuredTaskScope`) to manage the lifecycle of concurrent subtasks.
- Avoid `synchronized` blocks where possible — prefer `ReentrantLock`, `StampedLock`, or lock-free data structures
  from `java.util.concurrent`.

---

## Code Style & Design

### General

- Follow **SOLID principles** rigorously.
- Prefer **composition over inheritance**.
- Keep classes and methods **small and focused** (Single Responsibility Principle).
- Avoid deeply nested code — flatten with early returns and guard clauses.
- Use **meaningful names** — no abbreviations, no single-letter variables except in lambdas/streams where context is
  clear.
- Limit method parameters to 3 or fewer; use parameter objects/records when more are needed.
- Avoid magic numbers and magic strings — use named constants or enums.

### Immutability & Safety

- Prefer **immutable objects** — use records, `final` fields, and unmodifiable collections.
- Use `Optional` to represent the absence of a value; never return `null` from public APIs. Do not use `Optional`
  as a method parameter or field type.
- Never use `null` as a method argument intentionally.
- Use `List.copyOf()`, `Map.copyOf()`, `Set.copyOf()` when returning defensive copies.

### Functional Style

- Prefer declarative code with streams and lambdas over imperative loops where readability is maintained.
- Avoid side effects in stream pipelines.
- Use method references when they improve clarity.

### Error Handling

- Use **unchecked exceptions** for programming errors; create domain-specific exception types.
- Never swallow exceptions silently — always log or rethrow.
- Include meaningful messages and causes in exceptions.
- Never return or pass `null` — use `Optional`, empty collections, or throw an exception.

---

## Architecture & Patterns

- Structure code in clear **layers**: domain model, application services, infrastructure/adapters.
- Depend on **abstractions (interfaces)**, not concrete implementations.
- Apply the **Ports & Adapters (Hexagonal Architecture)** pattern for boundary separation.
- Use **dependency injection** — avoid static state and service locators.
- Prefer **factory methods** or **builders** over complex constructors.
- Design with **events** — publish domain events for side effects rather than coupling services directly.

---

## Testing

- Write **unit tests** for all business logic using **JUnit 5**.
- Use **AssertJ** for fluent, readable assertions.
- Use **Mockito** for mocking dependencies.
- Follow the **Arrange-Act-Assert (AAA)** pattern.
- Name tests clearly: `methodUnderTest_givenCondition_expectedBehavior()` or descriptive `@DisplayName` annotations.
- Prefer **test-driven development (TDD)** — write the test first.
- Use `@ParameterizedTest` to cover multiple input scenarios concisely.
- Use **Testcontainers** (via `prefab-test`) for integration tests that require real infrastructure.
- Integration tests must be independent, idempotent, and not rely on shared mutable state.
- Follow the **F.I.R.S.T.** principles: Fast, Independent, Repeatable, Self-Validating, Timely.
- Never leave a failing or flaky test — fix or remove it with a documented reason.

---

## Maven & Dependencies

- Keep dependencies **minimal and purposeful** — add nothing without justification.
- Always specify explicit versions; avoid version drift.
- Use well-maintained, widely-adopted libraries.
- Do not introduce dependencies with known CVEs. Use the CVE Remediator agent to verify new dependencies.
- Pin transitive dependency versions explicitly when security or compatibility requires it.

---

## Logging & Observability

- Use **SLF4J** as the logging facade.
- Log at appropriate levels: `DEBUG` for diagnostics, `INFO` for lifecycle events, `WARN` for recoverable issues,
  `ERROR` for failures.
- Never log sensitive data (passwords, tokens, PII).
- Use structured log messages with contextual parameters — no string concatenation.

---

## Documentation

- Write **Javadoc** for all public types and methods in shared/library code.
- Document *why*, not *what* — the code explains what; comments explain intent and constraints.
- Keep comments up to date; stale comments are worse than no comments.

---

## Security

- **Never hardcode** secrets, passwords, API keys, or tokens.
- Validate and sanitize all external inputs at the boundary (HTTP, Kafka, query parameters).
- Follow the **principle of least privilege**.
- Use parameterized queries and ORM-managed queries — never concatenate SQL strings.
- Keep dependencies up to date and regularly audit for CVEs.
- Do not log sensitive fields; apply `@JsonIgnore` or dedicated response DTOs to prevent accidental exposure.
- See [section 14 of AGENTS.md](../AGENTS.md#14-security-first) for the full rules.

---

## Craftsmanship & Ownership

### Code Quality

- **Eliminate all compiler warnings** — treat warnings as errors.
- Remove dead code, unused imports, unused variables, and redundant casts immediately.
- Apply consistent formatting throughout — max 120 characters per line, LF line endings (`\n`).

### Refactoring (Boy Scout Rule)

- Leave the code cleaner than you found it.
- Once tests are green, ask: *"Is this the clearest, simplest way to express this logic?"*
- Extract methods, introduce well-named abstractions, and eliminate duplication (DRY).
- Fix failing tests, regardless of whether you broke them or not.

### Final Checklist Before Considering Work Done

- [ ] All tests pass reliably
- [ ] Zero compiler warnings
- [ ] No unused code or imports
- [ ] Code reads naturally — a peer should understand it without explanation
- [ ] Logging and error messages are meaningful and actionable
- [ ] Edge cases are handled and tested
- [ ] Developer Guide updated if a Prefab feature was added or changed

---

## Issue Reporting

- When a task reveals a problem outside its scope, create a new backlog task instead of silently ignoring it or
  over-extending the fix.
- See [section 15 of AGENTS.md](../AGENTS.md#15-report-issues) for the full rules.
- Clean up any temporary files, databases, or resources created during code generation or testing.
