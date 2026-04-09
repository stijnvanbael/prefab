---
id: TASK-107
title: Data export endpoints (CSV and Excel)
status: To Do
assignee: []
created_date: '2026-04-09 15:28'
labels:
  - "\U0001F4E6feature"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
"Can you give me this as an Excel file?" is one of the most common requests in any business application. Finance teams, operations staff, and managers routinely need to pull data into spreadsheets for analysis or reporting. Currently Prefab generates JSON list endpoints but offers no way to export the same data as CSV or XLSX without hand-writing a controller.

A new `@Export` annotation should generate a download endpoint that streams the same data as the corresponding `@GetList` endpoint – applying the same filters, ordering, and security rules – but serialised as CSV or XLSX.

Example usage:

```java
@Aggregate
@GetList
@Export(formats = {ExportFormat.CSV, ExportFormat.XLSX})  // GET /invoices/export?format=csv
public record Invoice(
    @Id Reference<Invoice> id,
    @Version long version,
    String invoiceNumber,
    @Filter String status,
    BigDecimal amount,
    Instant issuedAt
) { ... }
```

The generated endpoint:
- `GET /${resource}/export?format=csv` or `?format=xlsx`
- Accepts all the same `@Filter` parameters and sort/page parameters as the list endpoint (but defaults to no pagination so the full result set is exported)
- Returns the data with the appropriate `Content-Type` and `Content-Disposition: attachment` headers
- Column headers are derived from field names (converted to human-readable Title Case); the `@Export` annotation allows overriding column labels per field via a companion `@ExportColumn(label = "Invoice #")" annotation
- Uses Apache POI for XLSX generation (optional dependency, only required if XLSX is requested)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add @Export(formats = ...) annotation to prefab-core with an ExportFormat enum (CSV, XLSX) defaulting to CSV only
- [ ] #2 Add @ExportColumn(label = "...", order = N) annotation to prefab-core so developers can customise per-field column headers and ordering in the export output
- [ ] #3 The annotation processor generates an export controller method (GET /${resource}/export) that streams the full (unpaginated) dataset filtered by the same @Filter parameters as the corresponding @GetList endpoint
- [ ] #4 CSV output uses RFC 4180 format with a header row; the Content-Type is text/csv and Content-Disposition is attachment; filename="${resource}.csv"
- [ ] #5 XLSX output uses Apache POI (optional compile-time dependency in a new prefab-export module or as an optional dependency of prefab-core); Content-Type is application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- [ ] #6 Column headers default to the field name converted to Title Case (e.g. issuedAt → Issued At); @ExportColumn(label=...) overrides the header; fields annotated with @ExportColumn(exclude=true) are omitted
- [ ] #7 The generated export endpoint obeys the same @Security rules as the corresponding @GetList endpoint
- [ ] #8 An @Export annotation without a corresponding @GetList on the same aggregate produces a clear compiler error
- [ ] #9 Add annotation-processor unit tests for the ExportPlugin following the pattern of existing plugin tests
- [ ] #10 README updated with an 'Export' section showing the annotations and an example download URL
<!-- AC:END -->
