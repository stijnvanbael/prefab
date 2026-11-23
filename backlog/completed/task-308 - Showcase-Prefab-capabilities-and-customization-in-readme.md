---
id: TASK-308
title: Showcase Prefab capabilities and complex customization in the README
status: Done
assignee: []
created_date: '2026-11-16 15:43'
labels: [documentation, dx]
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Make `readme.md` a compelling, accurate entry point for adopters. Link the Developer Guide near the top, describe every area Prefab currently touches, and demonstrate how meaningful real-world customization fits its declarative model and supported escape hatches. Keep the quick start short; put depth in the guide rather than duplicating it.

Related: TASK-148 (authoritative guide), TASK-125 (agent learning path), TASK-171 (annotation compatibility). This story concerns README discovery and positioning, not implementing roadmap features.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A prominent Developer Guide link appears immediately below the title/intro, before the quick start.
- [x] #2 A capability inventory checked against modules, annotations, examples and existing guide pages covers domain modeling, REST and generated contracts, persistence/migrations/indexing, messaging/schema evolution, streams, security/tenancy/audit, validation/binary storage/search, test helpers, observability, infrastructure generation and extension points.
- [x] #3 Every capability has a working deep link to the relevant guide section; unsupported or planned capabilities are clearly distinguished from released behavior.
- [x] #4 At least three README code examples, grounded in source-linked scenarios, show combinations of features and customization, including repository queries, plugin generation and runtime provider/security customization without editing generated sources.
- [x] #5 Unsupported blanket claims about application complexity are replaced with evidence-based boundaries and trade-offs; setup/version/module examples remain accurate and links/snippets are checked.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Inventory shipped features and public customization hooks, select existing compilable examples, reorganize the README around discovery and use cases, and link missing depth to guide work rather than advertising unimplemented features.
<!-- SECTION:PLAN:END -->

## Analysis

- The current README has a quick-start example but places the Developer Guide after it, describes Prefab as generating an "entire application", and says it is not intended for very complex applications without defining a supported boundary.
- `backlog/docs/developer-guide.md` is already the canonical index for detailed docs; use its existing guide pages and anchors rather than duplicating feature explanations in the README.
- Repository-backed customization examples exist for repository mixins (`examples/kafka/.../ChannelSummaryRepositoryMixin.java` and `UserStatusRepositoryMixin.java`), generated plugin overrides (`@Generate`/`PrefabPlugin`), and runtime tenant, audit, and Spring Security providers/customizers.
- The Maven reactor targets Java 21 (`maven.compiler.release=21`), despite the repository instructions describing Java 25; README requirements should follow the actual build configuration.
- Most requested capabilities have canonical guide anchors; Terraform infrastructure generation exists as the `prefab-terraform` module but is not yet described in the module guide, so add that missing guide section before linking it from the README.
- The source checkout is `0.13.3-SNAPSHOT` and includes release tags through `0.13.2`; describe the capability inventory as current-source behavior and distinguish it from functionality present in any particular published artifact.
- Backlog MCP tools are unavailable in this session, so this task file is being maintained directly as permitted by the repository workflow.

## Implementation Notes

- Replaced the README's broad application-generation claims with current-source capability inventory, explicit module/version caveats, and scope boundaries. Added a concise Java 21+/Spring Boot 4.x Maven quick start using the required core, persistence, and provided-scope annotation-processor dependencies.
- Added source-linked examples for a repository-mixin-backed event projection, per-aggregate plugin generation, and runtime tenant/audit/security customization that leaves generated source processor-owned.
- Added the missing GCP Terraform section to the module guide, including generated output scope and the compile-time limitation for unresolved Pub/Sub topic expressions.
- Validated local links and Markdown anchors in the README and module guide; `git diff --check` passes.

## Follow-up Analysis

- The user clarified that “showcase” requires more actual code examples in the README, not only links to repository sources. Replace the three short source-linked bullets with contextual, source-backed Java snippets demonstrating the repository mixin/projection, built-in plugin output customization, and runtime tenant/security extension points.

## Follow-up Implementation Notes

- Replaced the source-only customization bullets with three contextual README examples: a multicast event projection backed by a repository mixin, per-aggregate migration-plugin control, and JWT tenant resolution combined with method-level authorization.
- Added links from each example to the actual source and canonical guide sections. Clarified the tenant provider's `null` behavior so the example does not imply that missing claims remain tenant-isolated.
- Revalidated local Markdown links and anchors and ran `git diff --check`.
