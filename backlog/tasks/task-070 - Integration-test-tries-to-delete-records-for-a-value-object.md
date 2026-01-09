---
id: task-070
title: Integration test tries to delete records for a value object
status: Done
assignee: []
created_date: '2026-01-05 18:01'
updated_date: '2026-01-08 18:38'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 27000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
eg:
```
@Aggregate
@DbMigration
public record Student(
        @Id String id,
        @Version long version,
        @NotNull PersonName name,
        int yearOfBirth
)
```

Prefab integration tests try to delete all records from table `person_name`, but as PersonName is a value object, there is no such table.
<!-- SECTION:DESCRIPTION:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Happens when `@Embedded.Nullable` is missing on a value object. Fail compilation if so.
<!-- SECTION:NOTES:END -->
