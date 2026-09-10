---
id: TASK-283
title: Enabling AVSC records/enums to implement interfaces
status: To Do
assignee: []
created_date: '2026-09-10 09:42'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow individual AVSC-generated records or enums to implement an interface.

Requirements:
- Enable interface specification on the top-level event interface in the event hierarchy
- Match generated types to interfaces by namespace and name
- Support multiple records/enums implementing the same interface
- Generated code should declare the interface implementation

Acceptance Criteria:
- AVSC configuration accepts interface specifications (e.g. via annotation or metadata)
- Code generator matches types by namespace + name and adds implements clause
- Generated records and enums properly implement the specified interface
- Interface methods are available on generated types
- All existing tests pass with interface implementations
- Documentation updated with usage examples
<!-- SECTION:DESCRIPTION:END -->
