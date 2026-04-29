---
id: TASK-146
title: Path parameters derived from @Create and @Update method signatures
status: To Do
assignee: []
created_date: '2026-04-29 12:19'
updated_date: '2026-04-29 13:03'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When a @Create or @Update annotated method declares a parameter whose name matches a path variable in the annotation's path, that parameter should be treated as a path parameter and excluded from the generated request record. When all non-parent parameters are path-derived, no request record class should be generated.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Path variables in @Create/@Update path (e.g. /{name}) are detected by matching {paramName} tokens in the path string
- [ ] #2 Method parameters whose names match a path variable are excluded from the generated request record
- [ ] #3 When all non-parent parameters are path-derived, no request record class is generated for that method
- [ ] #4 Path-derived parameters are added as @PathVariable in the generated controller method
- [ ] #5 Path-derived parameters are passed directly to the service method, not extracted from the request record
- [ ] #6 The generated test client passes path-derived parameters as individual method arguments, not inside the request object
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Technical Analysis
### Overview
Path parameters are path template variables written as {paramName} in the annotation path string. Currently, the only path-derived exclusion logic is the parent-field detection (parentFieldName) in CreateServiceWriter and the implicit {id} variable for @Update. The goal is to generalise this mechanism so that any constructor/method parameter whose name matches a {...} token in the annotation's path is treated as a path variable.
### Path variable detection
Introduce a utility method (e.g. in ControllerUtil or a dedicated PathVariables helper) that parses {name} tokens from a path string and returns a Set<String>. For @Create the path comes from Create.path(); for @Update the path comes from Update.path() (suffix after /{id}).
### Changes to @Create flow
**CreatePlugin.writeAdditionalFiles()** - The existing guard !createConstructor.getParameters().isEmpty() must also exclude path variables before deciding whether to generate a request record. Use the new utility to detect path variables from Create.path(), then filter them out from the parameter list before the emptiness check.
**CreateControllerWriter.buildCreateMethod()** - After handling the parent path variable, detect path variables from create.path() and add each as a @PathVariable String parameter on the controller method. Exclude these from the odyParams list that feeds the request record type. Update uildServiceArgs() to pass path variable params directly alongside (or instead of) the request record.
**CreateServiceWriter.addConstructorArgs()** - Accept a set of path variable names. For each path variable, add a String pathVarName parameter to the generated service method. Map these directly to constructor arguments (like the parent-field logic), bypassing the request record.
**CreateRequestRecordWriter.writeRequestRecord()** - Receive the set of path variable names and filter them out (on top of the existing parent-field filter) before deciding whether the record is non-empty.
**CreateTestClientWriter.createMethods()** - Detect path variables from the annotation, add them as individual String parameters to the generated test-client method, and exclude them from the request object. Pass them in the URL path (interpolated into the path template).
### Changes to @Update flow
**UpdateManifest** - Add a List<VariableManifest> pathParameters field to hold parameters that are derived from Update.path() tokens.
**UpdatePlugin.updateMethodsOf()** - After computing llParams, parse path tokens from update.path(), match against param names, and populate both the new pathParameters field and exclude them from equestParameters.
**UpdateControllerWriter.updateMethod()** - After the fixed id path variable, iterate update.pathParameters() and add each as a @PathVariable String parameter on the controller method. Forward them to the service call.
**UpdateServiceWriter.updateMethod()** - Add each pathParameter as a String argument. Pass each directly to the domain method call (via esolveParam or a direct reference), not via the request record.
**UpdateTestClientWriter** - Include path parameters in the URL template and as individual method arguments alongside id.
### Request record omission
For @Create: if, after filtering the parent field **and** path variables, no parameters remain, skip writing the request record entirely (the guard already exists in CreatePlugin and CreateRequestRecordWriter; it just needs the extended filter).
For @Update: if update.requestParameters() is empty (which will be true when all non-parent params are path-derived), the already existing branch in UpdatePlugin.writeAdditionalFiles() skips record generation.
### Files to change
- ControllerUtil (or new PathVariables helper) - path token extraction utility
- CreatePlugin - pass path vars when deciding request-record generation
- CreateControllerWriter - add @PathVariable params, exclude from body
- CreateServiceWriter - accept path vars as individual method params
- CreateRequestRecordWriter - filter path vars from record fields
- CreateTestClientWriter - path vars as individual args + URL interpolation
- UpdateManifest - add pathParameters field
- UpdatePlugin.updateMethodsOf() - populate pathParameters, remove from equestParameters
- UpdateControllerWriter - add @PathVariable params
- UpdateServiceWriter - accept and pass path vars
- UpdateTestClientWriter - path vars in URL and method signature
### Test strategy
Add new test-resource source classes (similar to est/aggregate/source/) that declare @Create/@Update with path variables, and corresponding expected/ golden files. Register them in RestWriterTest. Cover: single path var, multiple path vars, path var + body param mix, and all-path-vars (no request record).
<!-- SECTION:PLAN:END -->
