---
id: task-165
title: "TypeMembers.backingFields() includes static fields, breaking builder generation for records with static constants"
status: "Done"
priority: "Medium"
labels: ["bug", "annotation-processor", "type-members", "reported-by:maestro"]
---

## Problem Statement

`TypeMembers.backingFields()` and `recordFieldsWithComponentAnnotations()` filter enclosed
elements by `ElementKind.FIELD` but do **not** also filter out `Modifier.STATIC` elements.
As a result, any `static final` field declared directly on a record type (e.g., well-known
constant instances) is mistakenly treated as an instance record component.

## Reproduction

Declare a single-value record with `static final` constant fields of the same type:

```java
public record AgentRole(@JsonValue String value) {

    public static final AgentRole PLANNER    = new AgentRole("PLANNER");
    public static final AgentRole RESEARCHER = new AgentRole("RESEARCHER");
    // … more constants …

    public static AgentRole of(String value) { return new AgentRole(value); }
}
```

Use `AgentRole` as a field in a Prefab `@Aggregate`:

```java
@Aggregate
public record SubTask(
        @Id Reference<SubTask> id,
        @Version long version,
        AgentRole assignedRole,
        …
) { … }
```

Prefab's `MotherWriter.writeNestedMother()` calls `type.fields()` on `AgentRole`, which
delegates to `TypeMembers.backingFields()`. That method collects **all** `ElementKind.FIELD`
enclosed elements, including the seven `static final AgentRole` constants.

The generated `AgentRoleBuilder` then has a builder method and field for each constant:

```java
// Generated — BROKEN
public class AgentRoleBuilder {
    private String value;
    private AgentRole PLANNER;      // ← static constant mistaken for instance field
    private AgentRole RESEARCHER;
    // …

    public AgentRole build() {
        return new AgentRole(value, PLANNER, RESEARCHER, …);  // ← wrong arity
    }
}
```

This fails to compile because `AgentRole` has only one constructor parameter (`String value`).

## Root Cause

In `TypeMembers.java`, `backingFields()` for non-records:

```java
return element.getEnclosedElements().stream()
        .filter(e -> e.getKind() == ElementKind.FIELD)   // ← missing static check
        .map(VariableElement.class::cast)
        …
```

And `recordFieldsWithComponentAnnotations()`:

```java
return element.getEnclosedElements().stream()
        .filter(e -> e.getKind() == ElementKind.FIELD)   // ← missing static check
        .map(VariableElement.class::cast)
        …
```

Both paths are missing `&& !e.getModifiers().contains(Modifier.STATIC)`.

Note: `ClassManifest.getFields()` already applies this filter correctly:

```java
.filter(element -> element.getKind() == ElementKind.FIELD
        && !element.getModifiers().contains(Modifier.STATIC))   // ← correct
```

## Proposed Fix

Add the static-modifier guard to both branches in `TypeMembers`:

```java
// backingFields() — non-record path
.filter(e -> e.getKind() == ElementKind.FIELD
        && !e.getModifiers().contains(Modifier.STATIC))

// recordFieldsWithComponentAnnotations()
.filter(e -> e.getKind() == ElementKind.FIELD
        && !e.getModifiers().contains(Modifier.STATIC))
```

## Acceptance Criteria

- [x] `TypeMembers.backingFields()` ignores `static` fields on non-record types
- [x] `TypeMembers.recordFieldsWithComponentAnnotations()` ignores `static` fields on record types
- [x] A record with static constant fields of its own type generates a correct `Builder`
      (only instance components appear as builder parameters)
- [x] Existing test suite passes
- [x] New unit test in `TypeMembersTest` (or equivalent) covers a record with static constants

## Current Workaround (Maestro)

Static constants moved out of `AgentRole` into a companion `AgentRoles` constants class
to avoid triggering the broken code path. See Maestro `AgentRoles.java`.

