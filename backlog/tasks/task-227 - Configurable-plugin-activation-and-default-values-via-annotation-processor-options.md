---
id: TASK-227
title: >-
  Configurable plugin activation and default values via annotation-processor
  options
status: To Do
assignee: []
created_date: '2026-05-21 12:13'
updated_date: '2026-05-22 17:44'
labels:
  - ✨feature
  - annotation-processor
dependencies: []
priority: medium
ordinal: 170000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Context

Prefab code generation is driven by `PrefabPlugin` implementations loaded via `ServiceLoader`. Currently, every plugin that is present on the compilation classpath is always active — there is no way for a user to disable an unwanted plugin or change its default behaviour without forking the code.

The only existing configurability is `prefab.builder.setterPrefix`, read through `processingEnv.getOptions()` in `PrefabContext`. The mechanism already works; it just needs to be generalised into a first-class plugin configuration API.

## Goal

Provide a structured, documented way for users to:

1. **Toggle individual plugins on or off** — e.g. disable `EventSchemaDocumentationPlugin` (AsyncAPI generation) or `DbMigrationPlugin` without removing the JAR from the classpath.
2. **Set default values / pick flavours** — e.g. choose the default HTTP method flavour, set the default event platform, or control whether builders include a `copy()` method.

## Alternatives considered

### Option A — Annotation-processor compiler arguments (`-A` flags)  ✅ *Recommended*

Pass options through the standard `javax.annotation.processing` option mechanism (`@SupportedOptions`, `processingEnv.getOptions()`). Already used for `prefab.builder.setterPrefix`.

```xml
<!-- Maven -->
<compilerArg>-Aprefab.plugin.asyncapi.enabled=false</compilerArg>
<compilerArg>-Aprefab.plugin.dbmigration.enabled=false</compilerArg>
<compilerArg>-Aprefab.event.defaultPlatform=KAFKA</compilerArg>
```

**Pros**: zero extra files; works in any build tool; discoverable via `@SupportedOptions`; testable by passing options to the compile task in unit tests.  
**Cons**: long option names can be tedious to type; no IDE auto-complete without a separate plugin.

### Option B — `prefab.yml` / `prefab.properties` file in project root or `src/main/resources`

A YAML or properties file that is picked up by `PrefabProcessor` at the start of each round.

```yaml
plugins:
  asyncapi: false
  dbmigration: false
defaults:
  event.platform: KAFKA
```

**Pros**: more readable for many settings; IDE can validate with a JSON schema.  
**Cons**: adds file-system I/O to the annotation processor; `Filer` API is tricky for reading (not writing) config files; path resolution differs between Maven, Gradle, and IDEs; harder to override per-profile.

### Option C — A `@PrefabConfig` annotation on the main application class

```java
@PrefabConfig(
    plugins = { @Plugin(id = "asyncapi", enabled = false) },
    defaults = { @Default(key = "event.platform", value = "KAFKA") }
)
@SpringBootApplication
@EnablePrefab
public class MyApplication { … }
```

**Pros**: configuration is co-located with the Spring entry point; compile-time validated.  
**Cons**: mixes runtime annotation (`@SpringBootApplication`) with compile-time configuration; harder to discover; requires a dedicated annotation type in the `core` module.

### Option D — `ServiceLoader`-based `PrefabConfiguration` SPI

Users provide a `PrefabConfiguration` implementation on the processor classpath (e.g. via a separate Maven module).

**Pros**: fully type-safe; extensible.  
**Cons**: heavyweight; unusual pattern; requires a separate JAR on the *annotation-processor* classpath.

## Recommended approach

Implement **Option A** as the primary mechanism because it is already partially in place, requires no new dependencies, and works uniformly across Maven, Gradle, and IDEs. Optionally complement it with **Option B** for projects with many settings.

Key design points:
- Introduce a `PrefabConfiguration` record (or class) in the `annotation-processor` module that centralises all option reading from `processingEnv.getOptions()`.
- `PrefabContext` holds a `PrefabConfiguration` instance and exposes typed accessors.
- Each plugin queries `context.configuration().isPluginEnabled("asyncapi")` (or similar) and short-circuits its hooks when disabled.
- `@SupportedOptions` on `PrefabProcessor` is extended to declare every supported key.
- Option names follow the pattern `prefab.<scope>.<key>` (e.g. `prefab.plugin.asyncapi.enabled`, `prefab.defaults.event.platform`).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A PrefabConfiguration record/class is introduced in the annotation-processor module that reads all supported -A options from processingEnv.getOptions()
- [ ] #2 PrefabContext holds and exposes a PrefabConfiguration instance via a typed accessor
- [ ] #3 Each built-in plugin checks whether it is enabled via context.configuration().isPluginEnabled(...) and skips all code-generation hooks when disabled
- [ ] #4 PrefabProcessor declares all supported option keys in @SupportedOptions so they are discoverable via javax.annotation.processing introspection
- [ ] #5 Option names follow the pattern prefab.plugin.<id>.enabled (boolean, default true) and prefab.defaults.<key> (string)
- [ ] #6 A user can disable the AsyncAPI documentation plugin by passing -Aprefab.plugin.asyncapi.enabled=false
- [ ] #7 A user can disable the DbMigration plugin by passing -Aprefab.plugin.dbmigration.enabled=false
- [ ] #8 The existing prefab.builder.setterPrefix option is migrated to PrefabConfiguration so all option handling is centralised
- [ ] #9 Unit tests verify that each plugin is skipped when its enabled flag is set to false
- [ ] #10 The configuration.md developer guide documents every supported option with its default value and an example Maven/Gradle snippet
<!-- AC:END -->
