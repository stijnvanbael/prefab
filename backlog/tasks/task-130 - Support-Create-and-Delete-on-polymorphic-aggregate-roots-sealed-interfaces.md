---
id: TASK-130
title: Support @Create and @Delete on polymorphic aggregate roots (sealed interfaces)
status: Done
assignee: []
created_date: '2026-04-18 14:38'
updated_date: '2026-04-29 14:51'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The annotation processor currently supports @Create and @Delete for regular @Aggregate record/class types, but not for polymorphic aggregate roots (sealed interfaces with nested record subtypes). The following gaps need to be filled in the annotation processor:

1. PolymorphicAggregateManifest — add parent() method
Add a method that finds the first common field across all subtypes annotated with @Parent.

2. PersistenceWriter.writePolymorphicRepository() — add RepositoryMixin and findByParent support
Mirror the existing writeRepository() behaviour: include @RepositoryMixin interfaces targeting the sealed interface type, and generate a findBy<Parent>(String parentId, Pageable pageable) method when a @Parent field is present.

3. DeletePlugin — add writePolymorphicController and writePolymorphicService overrides
When @Delete is present on the sealed interface, generate a DELETE /{id} controller method and a service delete(String id) method that calls repository.findById(id).ifPresent(a -> a.delete()) then repository.deleteById(id).

4. DeleteControllerWriter — add deleteMethodForPolymorphic(PolymorphicAggregateManifest, Delete)
Same as the existing deleteMethod(ClassManifest, Delete) but accepts a PolymorphicAggregateManifest.

5. DeleteServiceWriter — add deleteMethodForPolymorphic(PolymorphicAggregateManifest)
Generates: log statement, repository.findById(id).ifPresent(a -> a.delete()), repository.deleteById(id).

6. CreatePlugin — add writePolymorphicController, writePolymorphicService, and override writeAdditionalFiles
For each subtype that has a @Create-annotated constructor, generate one controller method, one service method, and one request record (if the constructor has parameters).

7. CreateControllerWriter — add createMethodForPolymorphic(PolymorphicAggregateManifest, ClassManifest, ExecutableElement, PrefabContext)
Method name: create<SubtypeName>. Path: /<kebab-subtype-name>s. Delegates to service.create<SubtypeName>(request). Redirect location: /<kebab-parent-name>/<id>.

8. CreateServiceWriter — add createMethodForPolymorphic(PolymorphicAggregateManifest, ClassManifest, ExecutableElement, PrefabContext)
Method name: create<SubtypeName>. Instantiates the subtype from the request record. Saves via the parent's repository. Returns aggregate.id().
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A sealed interface annotated with @Aggregate + @Delete generates a working delete endpoint and service method
- [ ] #2 Each nested record subtype annotated with @Create on its constructor generates its own POST endpoint under the parent controller, with URL /<kebab-subtype-plural> and redirect to /<kebab-parent-plural>/<id>
- [ ] #3 The generated polymorphic repository includes any matching @RepositoryMixin interfaces targeting the sealed interface type
- [ ] #4 The generated polymorphic repository includes a findBy<Parent> method when a common @Parent field exists
<!-- AC:END -->
