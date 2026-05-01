---
id: TASK-154
title: Spike — Prefab Flutter
status: In Progress
assignee: []
created_date: '2026-05-01 05:46'
updated_date: '2026-05-01 18:04'
labels:
  - spike
  - flutter
  - frontend
priority: medium
ordinal: 154000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate and design **Prefab Flutter**: a code-generation framework for Flutter applications that is
conceptually analogous to Prefab for Java/Spring Boot.

Just as Prefab for Java lets you annotate a plain record and have a full Spring MVC controller, service,
repository, DTOs, and database migrations generated at compile time, Prefab Flutter should let you
annotate (or declaratively describe) a model and have a complete Flutter frontend scaffold generated —
covering the common modern frontend patterns: list views, detail/edit forms, navigation, state
management, and API integration.

### Scope of the spike

1. **Survey the landscape** — research existing Flutter code-generation tools (`freezed`, `json_serializable`,
   `build_runner`, `mason`, Riverpod generators, etc.) and how they relate to what Prefab Flutter would offer.
2. **Identify target patterns** — determine the core frontend patterns to support in the first iteration:
   - List screen (paginated, filterable, sortable)
   - Detail screen (read-only and editable)
   - Create / Update forms with validation
   - Navigation / routing (go_router or Navigator 2.0)
   - State management layer (Riverpod / BLoC / Provider)
   - REST API client integration (auto-generated from OpenAPI / Prefab backend annotations)
3. **Propose an annotation / DSL API** — define what the developer writes (annotations on Dart classes,
   a YAML/JSON model, or a Dart DSL) to trigger generation. Align the concept with how Prefab Java
   annotations work (high-level intent, sensible defaults, opt-in overrides).
4. **Define generated artefacts** — specify what files are generated per model entity (screens, widgets,
   providers/BLoCs, API client, routing wiring).
5. **Propose the build pipeline** — decide whether generation is via Dart `build_runner`, a standalone CLI,
   or a separate tool (similar to annotation-processor in the Java world).
6. **Prototype / proof of concept** — implement a minimal working prototype that generates at least a
   list screen and a create form from a simple annotated Dart model or YAML spec.
7. **Document findings** — produce a concise design document (`backlog/docs/prefab-flutter-design.md`)
   covering the proposed API, architecture decisions, and open questions.
8. **Create follow-up tasks** — break the accepted design into concrete implementation backlog tasks.

### Also covers Prefab Frontend (general)

Insights from this spike also apply to a potential framework-agnostic "Prefab Frontend" layer (React,
Angular, Vue). Note common patterns, concerns, and decisions that would generalise beyond Flutter so
they can inform a broader Prefab Frontend initiative.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A landscape survey document or section in the design doc lists relevant existing Flutter code-generation tools and explains how Prefab Flutter differs from / builds on them
- [x] #2 A proposed annotation/DSL API is documented with at least one concrete example showing how a developer describes a model and what gets generated
- [x] #3 The design doc defines the full set of generated artefacts for a single model entity (screens, widgets, state management, API client stub, routing)
- [x] #4 The build pipeline approach (build_runner plugin, standalone CLI, or other) is evaluated and a recommendation with rationale is documented
- [x] #5 A minimal proof-of-concept generates a working Flutter list screen and create form from a sample annotated Dart class or YAML spec
- [x] #6 Design document `backlog/docs/prefab-flutter-design.md` is committed, covering API design, architecture decisions, open questions, and notes on generalising to Prefab Frontend
- [x] #7 Follow-up implementation tasks are created in the backlog, covering at minimum: annotation/DSL parser, code generators for each artefact type, CLI or build_runner integration, and example app (see TASK-155 through TASK-159)
- [x] #8 A pull request containing the design document, prototype code (if any), and follow-up tasks is open for review
<!-- AC:END -->
