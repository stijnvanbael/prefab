---
id: TASK-265
title: Customize the plural form of an aggregate root
status: Done
assignee: []
created_date: '2026-07-10 13:58'
updated_date: '2026-07-31 20:02'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab derives plural forms of aggregate root names with org.javalite.common.Inflector.pluralize(), which only knows English inflection rules. For domain terms with irregular plurals or non-English aggregate names, the generated plural is wrong and cannot be corrected today. Allow developers to declare a custom plural form on an aggregate root (e.g. an attribute on @Aggregate) that overrides the inflected default everywhere Prefab generates a plural.

Generated plurals are currently used in:
- REST controller base paths (ControllerUtil.pathOf, kebab-cased plural), including the parent segment of child aggregate paths (/parents/{parentId}/children)
- Polymorphic aggregate paths (HttpWriter.polymorphicPathOf, ControllerUtil.pathOf(PolymorphicAggregateManifest))
- Redirect/Location paths after create (CreateControllerWriter, CreateOrUpdateControllerWriter)
- OpenAPI @Operation summaries ("List Xs") in GetListControllerWriter
- Generated test client method names (findXs) in GetListTestClientWriter
- Debug log statements in generated services (GetListServiceWriter)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A developer can declare a custom plural form on an aggregate root, and the default Inflector-based pluralization applies when none is declared
- [x] #2 Generated REST controller base paths use the custom plural (kebab-cased), including the parent path segment of child aggregates whose parent has a custom plural
- [x] #3 Paths of polymorphic aggregates use the custom plural declared on the polymorphic root
- [x] #4 Redirect/Location paths returned after create and create-or-update operations use the custom plural
- [x] #5 OpenAPI operation summaries for list endpoints use the custom plural
- [x] #6 Generated test client method names for list/search endpoints use the custom plural
- [x] #7 Debug log messages in generated services use the custom plural
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Added Aggregate.plural() (default "", core module), used wherever Prefab derives a plural. TypeManifest.plural() centralizes the lookup (custom @Aggregate(plural=...) if present and non-blank, else Inflector.pluralize(simpleName())); ClassManifest.plural() and PolymorphicAggregateManifest.plural() delegate to it. ControllerUtil.pathOf(...) extracted a shared parentPathSegment(VariableManifest) helper that now resolves the parent aggregate's own plural (via the parent field's generic type parameter TypeManifest), fixing custom plurals on parent aggregates for nested child paths. Replaced all direct Inflector.pluralize(manifest.simpleName()) call sites (HttpWriter, ControllerUtil, CreateControllerWriter, CreateOrUpdateControllerWriter, GetListControllerWriter, GetListServiceWriter, GetListTestClientWriter) with manifest.plural()/polymorphic.plural().
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Aggregate roots can now declare @Aggregate(plural = "...") to override the Inflector-derived plural everywhere Prefab generates one: REST base paths (including parent path segments for child aggregates), polymorphic aggregate paths, create/create-or-update redirect paths, OpenAPI list summaries, generated test client method names, and service debug logs. Default behavior (Inflector-based pluralization) is unchanged when no override is declared.
<!-- SECTION:FINAL_SUMMARY:END -->
