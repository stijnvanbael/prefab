---
id: M-008
title: Add built-in Money and Quantity value types
status: To Do
assignee: []
created_date: '2026-05-08 16:38'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add built-in `Money` and `Quantity` value types to `prefab-core` to prevent the common `double price` precision bug. Both types should map automatically to PostgreSQL `NUMERIC` columns and have full Avro and JSON support.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Money type is backed by BigDecimal + currency code with automatic NUMERIC column mapping in PostgreSQL
- [ ] #2 Quantity type is backed by BigDecimal with automatic NUMERIC column mapping
- [ ] #3 Both types have JSON serialization/deserialization support
- [ ] #4 Both types are supported in Avro schema generation
- [ ] #5 Developer Guide documents both types with examples
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
