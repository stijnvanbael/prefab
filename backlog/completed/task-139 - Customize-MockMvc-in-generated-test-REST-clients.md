---
id: TASK-139
title: Customize MockMvc in generated test REST clients
status: Done
assignee: []
created_date: '2026-04-27 08:51'
updated_date: '2026-04-30 06:04'
labels: []
dependencies: []
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The generated test REST clients (e.g. CategoryClient, UserClient) build their MockMvc instance using MockMvcBuilders.webAppContextSetup(context).build() with no way to apply custom configuration. Test authors who need to add security filters, custom result handlers, or other MockMvc configurers (e.g. Spring Security's springSecurity()) have no hook to do so today.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Generated client constructors accept an optional list of MockMvcConfigurer beans from the Spring context
- [x] #2 When no MockMvcConfigurer beans are present the generated client behaves exactly as before
- [x] #3 The apply order of MockMvcConfigurer beans is deterministic (e.g. ordered by bean name or Spring @Order)
<!-- AC:END -->
