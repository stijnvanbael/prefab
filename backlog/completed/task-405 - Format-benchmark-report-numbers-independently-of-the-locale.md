---
id: TASK-405
title: Format benchmark report numbers independently of the locale
status: Done
assignee: []
created_date: '2026-11-27 09:45'
labels:
  - testing
dependencies: []
references:
  - benchmark/src/main/java/be/appify/prefab/benchmark/MarkdownReport.java
  - benchmark/src/main/java/be/appify/prefab/benchmark/Statistics.java
priority: low
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-399. `MarkdownReport` and `Statistics` formatted numbers with the JVM's default locale, so on a Mac set
to `en_BE` a report read `$0,25` and `1.150` tokens, and `ReportTest.markdownSummarisesPassRateMetricsAndFailures`
failed on `main`. Reports written on different machines must look the same so they can be compared.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Report numbers (cost, tokens, medians, pass-rate intervals, durations) use `Locale.ROOT`
- [x] #2 `ReportTest` passes regardless of the machine locale
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
`MarkdownReport` formats through a private `format(pattern, arguments)` helper that uses `Locale.ROOT`; `Statistics`
calls `String.format(Locale.ROOT, ...)` directly, because its `Interval.format()` method would shadow such a helper.
Verified on an `en_BE` machine: the benchmark module's 70 tests pass.
<!-- SECTION:NOTES:END -->
