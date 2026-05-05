# GitHub Copilot Instructions

When generating code in this repository, follow all guidelines defined in [AGENTS.md](../AGENTS.md).

Key principles to honor:

- **Security First**: Never hardcode secrets, validate all external input, avoid dependencies with known CVEs,
  apply least-privilege access, mask sensitive data in logs, use parameterised queries, and default to the most
  restrictive configuration. See [section 14 of AGENTS.md](../AGENTS.md#14-security-first) for the full rules.
- **Be Careful**: Avoid generating code that could cause harm, such as deleting data, sending emails, or making network requests.
  If such code is necessary, ask for explicit confirmation before generating it, and include clear warnings in the code comments.
- **Meaningful Names**: Use intention-revealing, pronounceable, searchable names. Classes are nouns, methods are verbs.
- **Small Functions**: Each function does one thing, has few arguments, no flag parameters, and follows Command-Query Separation.
- **Self-Documenting Code**: Prefer clear code over comments. Remove commented-out code.
- **Formatting**: Max 120 characters per line, LF line endings (`\n`), vertical openness between concepts.
- **Error Handling**: Use exceptions, not return codes. Never return or pass `null`.
- **Clean Tests**: Fast, Independent, Repeatable, Self-Validating, Timely (F.I.R.S.T.).
- **SRP & DI**: One responsibility per class, depend on abstractions, inject dependencies.
- **No Speculation**: Do not add complexity for hypothetical future needs.
- **Boyscout rule**: Leave the code cleaner than you found it. Refactor when you see an opportunity. Fix failing tests, regardless of whether you broke them or not.
- **Report Issues**: When a task reveals a problem outside its scope, create a new backlog task instead of
  silently ignoring it or over-extending the fix. See [section 15 of AGENTS.md](../AGENTS.md#15-report-issues) for the full rules.
- **Terminal truncation**: Keep terminal commands shorter than 256 characters to avoid truncation issues.
- **Clean up after yourself**: If you create temporary files, databases, or other resources during code generation or testing, make sure to clean them up afterwards to avoid clutter and potential security risks.