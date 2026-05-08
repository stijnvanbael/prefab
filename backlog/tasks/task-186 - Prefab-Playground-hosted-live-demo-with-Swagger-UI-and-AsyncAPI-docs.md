---
id: task-186
title: Prefab Playground — hosted live demo with Swagger UI and AsyncAPI docs
status: To Do
assignee: []
created_date: '2026-05-08'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prospective adopters must currently clone the repository, install Java 21, Maven, and Docker just to
see Prefab in action. A hosted "Prefab Playground" removes every barrier between "I heard about this"
and "I can see it working right now."

The Playground is a small, purpose-built Spring Boot application (similar in spirit to Spring
PetClinic) that showcases a realistic domain — e.g. an order-management system with `Product`,
`Order`, `Customer`, and `Shipment` aggregates — and is deployed publicly. It demonstrates:

- **Swagger UI** (`prefab-openapi`) showing all generated REST endpoints with live try-it-out
- **AsyncAPI UI** (`prefab-async-api`) visualising the event topology
- **Mermaid architecture diagram** (task-185) embedded in the landing page
- A read-only tour page explaining which Prefab annotation produced which endpoint or event

The application is deployed automatically on every Prefab release via a GitHub Actions workflow and
hosted on a free-tier platform (e.g. Railway, Render, or Fly.io). The source code lives in the
existing `examples/` directory and doubles as a comprehensive reference implementation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A playground example module exists under `examples/playground` with at least 3 aggregates and 2 event flows
- [ ] #2 Swagger UI is accessible at the public URL and all generated endpoints are listed and callable
- [ ] #3 AsyncAPI UI is accessible and shows the full event topology
- [ ] #4 A landing page explains what Prefab is and maps each visible endpoint back to the annotation that generated it
- [ ] #5 The Mermaid architecture diagram is embedded on the landing page
- [ ] #6 A GitHub Actions workflow deploys the playground automatically on every release tag
- [ ] #7 The playground URL is linked from the repository README and the Developer Guide
- [ ] #8 No real infrastructure credentials are hardcoded; the playground uses the in-memory test backend (task-175) or a free-tier managed database
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->

