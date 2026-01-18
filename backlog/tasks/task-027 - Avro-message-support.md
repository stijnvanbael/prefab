---
id: task-027
title: Avro message support
status: To Do
assignee: []
created_date: '2025-10-10 13:39'
updated_date: '2026-01-15 10:03'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 227.50377655029297
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
With generic records, mapped on the event records with a custom serializer that does the mapping and delegates to the Avro serializer. The custom serializer is wrapped in another serializer that is configured to select the right serializer/deserializer (JSON or Avro) for each event/topic. Generate the configuration.

SerDes hierarchy:

- FormatSelector
  - Json
  - AvroMapping
    - Avro
<!-- SECTION:DESCRIPTION:END -->
