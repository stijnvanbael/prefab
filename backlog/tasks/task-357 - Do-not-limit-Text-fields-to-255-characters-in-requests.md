---
id: TASK-357
title: Do not limit @Text fields to 255 characters in requests
status: To Do
assignee: []
created_date: '2026-11-21 18:10'
labels:
  - rest
  - validation
dependencies: []
priority: medium
type: bug
ordinal: 196000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`@Text` maps a `String` field to an unbounded `TEXT` column, but the generated create and update requests add
`@Size(max = 255)` to every `String` parameter without an explicit `@Size`. `@Text` cannot be put on a constructor or
method parameter (it targets fields and record components), so the processor never sees it there and a `@Text`
description is still rejected above 255 characters over REST.

Found while building the showcase catalog (TASK-337.03); `Product.description` works around it with an explicit
`@Size(max = MAX_DESCRIPTION_LENGTH)` on the parameters.

Either let the request parameter inherit `@Text` from the field it initialises (same name), or allow `@Text` on
parameters, and skip the default `@Size(max = 255)` for such parameters.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A create or update parameter for a @Text field accepts more than 255 characters without an explicit @Size
- [ ] #2 Covered by an annotation processor test
<!-- AC:END -->
