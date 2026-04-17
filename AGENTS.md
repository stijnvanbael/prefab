# Backlog manipulation

For manipulating backlog items, see [BACKLOG.md](BACKLOG.md).

# Agent Coding Guidelines

All agents working in this repository must follow the **Clean Code** principles as described by Robert C. Martin
("Uncle Bob") in *Clean Code: A Handbook of Agile Software Craftsmanship*.

---

## 1. Meaningful Names

- **Use intention-revealing names**: Variable, method, and class names should clearly communicate *why* they exist,
  *what* they do, and *how* they are used.
- **Avoid disinformation**: Do not use names that mislead or suggest a wrong type/concept (e.g., avoid naming a
  `HashSet` as `accountList`).
- **Make meaningful distinctions**: Avoid noise words like `data`, `info`, `manager`, `processor` unless truly
  necessary.
- **Use pronounceable names**: Names should be easy to say out loud in a code review.
- **Use searchable names**: Prefer named constants over magic numbers and single-letter variables (except loop
  counters).
- **Avoid encodings**: Do not use Hungarian notation or type prefixes (e.g., `strName`, `iCount`).
- **Classes should be nouns**, methods should be **verbs**.

---

## 2. Functions

- **Small**: Functions should be small — ideally fewer than 20 lines.
- **Do one thing**: A function should do one thing, do it well, and do it only.
- **One level of abstraction per function**: Do not mix high-level logic with low-level detail in the same function.
- **Avoid side effects**: A function should not have hidden effects outside its stated purpose.
- **Prefer fewer arguments**: Aim for zero to two parameters. More than three is a strong code smell.
- **Avoid flag arguments**: Do not pass booleans to control branching inside a function — split into two functions
  instead.
- **Command-Query Separation**: A function should either *do something* (command) or *answer something* (query),
  never both.
- **DRY — Don't Repeat Yourself**: Extract duplicated logic into reusable, well-named functions.

---

## 3. Comments

- **Good code is self-documenting**: Prefer clear code over explanatory comments.
- **Do not comment bad code — rewrite it.**
- **Acceptable comments**:
    - Legal/copyright headers
    - Explanation of intent when the code alone cannot convey it
    - Clarification of a non-obvious algorithm or constraint
    - TODO comments (but address them promptly)
    - Public API documentation (e.g., Javadoc)
- **Avoid**:
    - Redundant comments that restate what the code already says
    - Misleading or out-of-date comments
    - Commented-out code — delete it, version control has history

---

## 4. Formatting

- **Vertical openness**: Separate distinct concepts with blank lines.
- **Vertical density**: Keep closely related lines of code together without unnecessary spacing.
- **Vertical ordering**: High-level functions appear first, low-level details below (newspaper structure).
- **Horizontal formatting**: Keep lines short — aim for a maximum of 120 characters.
- **Consistent indentation**: Follow the project's established style; never mix tabs and spaces.
- **Team rules trump personal preference**: Agree on a style and apply it uniformly across the codebase.

---

## 5. Objects and Data Structures

- **Hide internal structure**: Objects expose behavior, not data. Use methods, not public fields.
- **Data Transfer Objects (DTOs)**: Simple data containers with no business logic are acceptable; do not mix with
  domain objects.
- **The Law of Demeter**: A method should only call methods of:
    - Its own class
    - Objects it creates
    - Objects passed as arguments
    - Objects held in instance variables
  Do **not** chain calls through foreign objects (`a.getB().getC().doWork()`).

---

## 6. Error Handling

- **Use exceptions, not return codes**: Throwing an exception is cleaner than returning and checking error codes.
- **Write the try-catch-finally block first**: Define the error boundary before filling in logic.
- **Use unchecked exceptions**: Checked exceptions break encapsulation and violate the Open/Closed Principle.
- **Provide context in exceptions**: Include enough information to understand the source and intent of the failure.
- **Do not return `null`**: Return empty collections, `Optional`, or throw an exception instead.
- **Do not pass `null`**: Avoid writing methods that accept `null` as a valid argument.

---

## 7. Boundaries

- **Isolate third-party code**: Wrap external libraries and APIs behind interfaces you control.
- **Write learning tests**: Write tests against third-party APIs to document expected behavior and catch breaking
  changes.
- **Depend on abstractions, not concretions**: This keeps boundaries clean and testable.

---

## 8. Unit Tests (TDD)

Follow the **Three Laws of TDD**:

1. You may not write production code until you have a failing unit test.
2. You may not write more of a unit test than is sufficient to fail.
3. You may not write more production code than is sufficient to pass the currently failing test.

**Clean test principles — F.I.R.S.T.**:

- **Fast**: Tests must run quickly.
- **Independent**: Tests must not depend on each other.
- **Repeatable**: Tests must produce the same result in any environment.
- **Self-Validating**: Tests must return a boolean pass/fail — no manual inspection.
- **Timely**: Tests should be written just before the production code they test.

**One assert per test** (where practical). Each test should verify a single concept.

---

## 9. Classes

- **Small**: Classes should be small — measured by *responsibilities*, not lines of code.
- **Single Responsibility Principle (SRP)**: A class should have one, and only one, reason to change.
- **High cohesion**: Instance variables should be used by most of the methods in the class. If not, split the class.
- **Open/Closed Principle (OCP)**: Classes should be open for extension but closed for modification.
- **Dependency Inversion**: Depend on abstractions (interfaces), not concrete implementations.

---

## 10. Systems

- **Separate construction from use**: The startup/wiring of objects (construction) must be separated from the
  runtime logic that uses them. Use dependency injection.
- **Use the simplest thing that works**: Do not over-engineer. Defer design decisions until the last responsible moment.
- **Emergent design**: Let good design emerge through continuous refactoring guided by tests.

---

## 11. General Smells and Heuristics to Avoid

| Smell                        | Guideline                                                                 |
|------------------------------|---------------------------------------------------------------------------|
| Long methods                 | Extract to smaller, well-named methods                                    |
| Large classes                | Split by responsibility                                                   |
| Long parameter lists         | Introduce parameter objects or builder patterns                           |
| Divergent change             | One class changed for many different reasons → split                      |
| Shotgun surgery              | One change requires edits across many classes → consolidate               |
| Feature envy                 | A method more interested in another class → move it                       |
| Data clumps                  | Groups of data that always appear together → extract to a class           |
| Primitive obsession          | Replace primitives with domain-specific types                             |
| Switch statements            | Replace with polymorphism                                                 |
| Parallel inheritance         | Every subclass of A requires a subclass of B → restructure                |
| Speculative generality       | Don't add complexity for hypothetical future needs                        |
| Temporary field              | Instance variables used only in some cases → extract to a dedicated class |
| Inappropriate intimacy       | Two classes that know too much about each other → introduce indirection   |
| Comments explaining bad code | Rewrite the code instead                                                  |

---

## Summary Principle

> *"Any fool can write code that a computer can understand.*
> *Good programmers write code that humans can understand."*
> — Martin Fowler (quoted in *Clean Code*)

**Leave the code cleaner than you found it — always apply the Boy Scout Rule.**

