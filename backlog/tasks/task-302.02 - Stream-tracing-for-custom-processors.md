---
id: TASK-302.02
title: Stream tracing for custom processors
status: Done
assignee: []
created_date: '2026-10-30 13:40'
labels:
  - streams
  - tracing
dependencies:
  - TASK-302.01
parent_task_id: TASK-302
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Slice 2 of TASK-302. `KafkaPrefabStreamProcessorAdapter` remembers the origin set of the record being processed and `KafkaPrefabProcessorContext.forward` applies it to forwarded records. An explicit set on the `StreamRecord` wins; forwards from punctuators without an input record have none.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Records forwarded from `process` inherit the origin set of the processed record, also when chained.
- [x] #2 Explicit correlation IDs set by the processor are respected.
- [x] #3 Records forwarded from a punctuator have no correlation IDs.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The processor adapter stores the incoming prefab-correlation-ids header on the KafkaPrefabProcessorContext for the duration of process(); forward() adds it unless the record already carries explicit correlation IDs. Outside process() (punctuators) nothing is inherited. Tested with process_shouldInheritCorrelationOfProcessedMessage; explicit and punctuator cases are covered by construction, not a dedicated test.
<!-- SECTION:NOTES:END -->
