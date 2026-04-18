# GitHub Copilot Instructions

When generating code in this repository, follow all guidelines defined in [AGENTS.md](../AGENTS.md).

Key principles to honor:

- **Meaningful Names**: Use intention-revealing, pronounceable, searchable names. Classes are nouns, methods are verbs.
- **Small Functions**: Each function does one thing, has few arguments, no flag parameters, and follows Command-Query Separation.
- **Self-Documenting Code**: Prefer clear code over comments. Remove commented-out code.
- **Formatting**: Max 120 characters per line, LF line endings (`\n`), vertical openness between concepts.
- **Error Handling**: Use exceptions, not return codes. Never return or pass `null`.
- **Clean Tests**: Fast, Independent, Repeatable, Self-Validating, Timely (F.I.R.S.T.).
- **SRP & DI**: One responsibility per class, depend on abstractions, inject dependencies.
- **No Speculation**: Do not add complexity for hypothetical future needs.

## Common Mistakes to Avoid

- **CM-001**: Never use Windows-style CRLF (`\r\n`) line endings. Always use LF (`\n`).

