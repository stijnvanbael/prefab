---
id: TASK-148
title: Generate extensive developer guide for users and LLMs
status: To Do
assignee: [ ]
created_date: '2026-04-30 06:41'
updated_date: '2026-04-30 06:42'
labels:
  - documentation
  - agents
  - dx
dependencies: [ ]
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab needs a comprehensive developer guide that serves both human developers and LLMs as a single authoritative
reference. The guide must cover every annotation, generated artefact, supported type, module, configuration option, and
extension point. Where possible, parts of the guide should be generated from the source code (Javadoc, annotation
attributes, example modules) so the documentation stays in sync with the code automatically. The guide must be
maintained as a living document: any agent that changes Prefab behaviour must update the relevant section of the guide
as part of the same task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [ ] #1 Create backlog/docs/developer-guide.md as the single entry point, structured by module and feature
- [ ] #2 Document every public annotation with: purpose, all attributes with types and defaults, generated artefacts,
  and a minimal code example
- [ ] #3 Document every built-in type (Reference, Binary, AuditInfo, etc.) with purpose, usage, and database/JSON
  mapping
- [ ] #4 Investigate and implement generation of annotation attribute tables from Javadoc using a Maven plugin (e.g.
  maven-javadoc-plugin or custom doclet) so the attribute reference stays in sync with the code
- [ ] #5 Extract and embed runnable code snippets from the example modules (avro, kafka, mongodb, pubsub, sns-sqs) into
  the guide so examples are always compilable
- [ ] #6 Add a section describing every generated class (controller, service, repository, request/response records) with
  field-by-field explanation
- [ ] #7 Add an extension point guide: how to write custom plugins (processors, file writers, repository mixins) with a
  worked example
- [ ] #8 Add a module dependency matrix showing which prefab-* modules are required vs optional for each feature
- [ ] #9 Add a troubleshooting section that maps known error messages to their root causes and fixes
- [ ] #10 Add a rule to AGENTS.md requiring agents to update the relevant developer-guide section when they change or
  add any Prefab feature
- [ ] #11 Evaluate whether a Maven plugin can auto-generate the annotation reference section during the build and embed
  it into the guide (e.g. via maven-resources-plugin filtering or a custom plugin)

<!-- AC:END -->
