---
id: task-076
title: Flaky test in PubSub Example
status: In Progress
assignee: []
created_date: '2026-01-08 18:09'
updated_date: '2026-01-10 11:48'
labels:
  - "\U0001F41Ebug"
dependencies: []
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
MessageIntegrationTest

```
org.awaitility.core.ConditionTimeoutException: Assertion condition defined as a Lambda expression in be.appify.prefab.example.pubsub.message.MessageIntegrationTest 
Expecting ArrayList:
  [[], [], []]
to contain:
  [[UnreadMessage[message=SimpleReference[id=c7fc0948-130c-4249-b3d2-02a8db7e8107], channel=SimpleReference[id=b4b7d79a-be8a-4d70-9c50-b9fa1c513a78]]],
    [UnreadMessage[message=SimpleReference[id=c7fc0948-130c-4249-b3d2-02a8db7e8107], channel=SimpleReference[id=b4b7d79a-be8a-4d70-9c50-b9fa1c513a78]]]]
but could not find the following element(s):
  [[UnreadMessage[message=SimpleReference[id=c7fc0948-130c-4249-b3d2-02a8db7e8107], channel=SimpleReference[id=b4b7d79a-be8a-4d70-9c50-b9fa1c513a78]]]]
 within 5 seconds.
```
<!-- SECTION:DESCRIPTION:END -->
