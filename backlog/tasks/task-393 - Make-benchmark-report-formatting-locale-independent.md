---
id: TASK-393
title: Make benchmark report formatting locale-independent
status: To Do
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
- [ ] #1 Report formatting uses an explicit locale (`Locale.ROOT` or `Locale.US`) for numbers and currency.
- [ ] #2 `ReportTest` passes regardless of the JVM default locale (covered by a test that sets a non-English default).
<!-- AC:END -->
