---
id: TASK-089
title: 'Spring Data JDBC: Wrong table name for inner classes'
status: To Do
assignee: []
created_date: '2026-02-07 19:39'
updated_date: '2026-02-07 19:39'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 227.80418395996094
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Spring Data JDBC by default only uses the last part in the class name for the table name while Prefab expects `outer_inter` as the table name for proper namespacing. This can be worked around with `@Table`, but find a way to make the qualified name the default
<!-- SECTION:DESCRIPTION:END -->
