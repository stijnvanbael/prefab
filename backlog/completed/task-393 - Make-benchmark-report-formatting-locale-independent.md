---
id: TASK-393
title: Make benchmark report formatting locale-independent
status: Done
assignee: []
created_date: '2026-11-26 10:00'
labels:
  - testing
  - benchmark
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`ReportTest.markdownSummarisesPassRateMetricsAndFailures` fails on a machine with a Dutch/Belgian default locale: the
report renders cost as `$0,50` and token counts as `2.300` instead of `$0.50` and `2,300`. The test passes with
`-Duser.language=en -Duser.country=US`, so `mvn verify` only passes where the JVM default locale is English (CI).
Benchmark reports are shared artefacts and should not depend on the machine that wrote them.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Report formatting uses an explicit locale (`Locale.ROOT` or `Locale.US`) for numbers and currency.
- [x] #2 `ReportTest` passes regardless of the JVM default locale (covered by a test that sets a non-English default).
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
`MarkdownReport` already formats numeric values with `Locale.ROOT`, and `Statistics` does the same (implemented under
TASK-405). The missing acceptance-criteria coverage is a `ReportTest` that temporarily sets a non-English JVM default
locale and verifies formatted cost and token counts remain invariant. Restore the original locale in a `finally` block
to avoid leaking process-global state to other tests. The test uses JUnit's locale resource lock and verifies the
summary and per-attempt numbers, cost, and pass-rate interval under `nl-BE`. `mvn -pl benchmark -Dtest=ReportTest test`
passed (9 tests). The full benchmark module run also passed all 9 `ReportTest` cases, but failed in two unrelated,
platform-specific `IsolationTest` cases that expect POSIX paths and `/tmp` on this Windows host.
<!-- SECTION:NOTES:END -->
