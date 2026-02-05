---
id: TASK-083
title: HTTP 403 response on unauthenticated requests when security is disabled
status: Done
assignee: []
created_date: '2026-01-21 10:13'
updated_date: '2026-01-22 10:38'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Spring 4 default security has been changed. Whenever the security jar is on the classpath, security is enabled.

Exclude Spring security starter from the core, add manually to enable security.
<!-- SECTION:DESCRIPTION:END -->
