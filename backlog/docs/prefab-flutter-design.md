# Prefab Flutter — Design Document

**Status:** Spike / Proposal | **Author:** Prefab Team | **Date:** 2026-05-01

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Landscape Survey](#2-landscape-survey)
   - [2.1 Existing Flutter Code-Generation Tools](#21-existing-flutter-code-generation-tools)
   - [2.2 Gap Analysis](#22-gap-analysis)
3. [Design Philosophy](#3-design-philosophy)
4. [Proposed Annotation API](#4-proposed-annotation-api)
   - [4.1 Entity-level Annotations](#41-entity-level-annotations)
   - [4.2 Operation Annotations](#42-operation-annotations)
   - [4.3 Field-level Annotations](#43-field-level-annotations)
   - [4.4 Complete Example](#44-complete-example)
5. [Generated Artefacts](#5-generated-artefacts)
   - [5.1 List Screen](#51-list-screen)
   - [5.2 Detail Screen](#52-detail-screen)
   - [5.3 Create / Edit Form Screens](#53-create--edit-form-screens)
   - [5.4 Riverpod Providers](#54-riverpod-providers)
   - [5.5 API Client](#55-api-client)
   - [5.6 go_router Routes](#56-go_router-routes)
6. [Build Pipeline](#6-build-pipeline)
   - [6.1 Option A — source_gen + build_runner Plugin](#61-option-a--source_gen--build_runner-plugin)
   - [6.2 Option B — Standalone CLI Tool](#62-option-b--standalone-cli-tool)
   - [6.3 Option C — YAML / JSON Model File](#63-option-c--yaml--json-model-file)
   - [6.4 Recommendation](#64-recommendation)
7. [Proof of Concept](#7-proof-of-concept)
   - [7.1 Package Structure](#71-package-structure)
   - [7.2 Developer Input](#72-developer-input)
   - [7.3 Generated Output Summary](#73-generated-output-summary)
8. [Generalising to Prefab Frontend](#8-generalising-to-prefab-frontend)
9. [Open Questions](#9-open-questions)
10. [Follow-up Tasks](#10-follow-up-tasks)

---

## 1. Executive Summary

**Prefab Flutter** is a code-generation framework for Flutter applications that follows the same
philosophy as Prefab for Java/Spring Boot: *express your domain model at a high level; get a
fully-wired, modern UI scaffold generated for free.*

Just as a Java developer annotates a `record` with `@Aggregate`, `@Create`, and `@GetList` to
receive a complete Spring MVC + Spring Data JDBC stack, a Flutter developer annotates a Dart class
with `@PrefabView`, `@PrefabCreate`, and field-level `@ListColumn` / `@FormField` annotations to
receive:

| Generated artefact | Annotation that triggers it |
|---|---|
| Paginated, searchable, sortable **list screen** | `@PrefabView` + `@ListColumn` on fields |
| Read-only **detail screen** | `@PrefabView` |
| Create **form screen** | `@PrefabCreate` |
| Edit **form screen** | `@PrefabUpdate` |
| Delete **confirmation dialog** | `@PrefabDelete` |
| Riverpod **`AsyncNotifier` provider** (full CRUD lifecycle) | `@PrefabView` |
| `dio`-based **REST API client** | `@PrefabApi` |
| Typed **`go_router` routes** | `@PrefabView` |

The generator runs as a standard `build_runner` plugin, so it integrates with the existing
Flutter toolchain without any new CLI tool or IDE plugin.

---

## 2. Landscape Survey

### 2.1 Existing Flutter Code-Generation Tools

| Tool | Purpose | Gap vs. Prefab Flutter |
|---|---|---|
| **`freezed`** | Immutable data classes, sealed unions | No UI generation; data layer only |
| **`json_serializable`** | JSON ↔ Dart serialization | No UI generation; data layer only |
| **`riverpod_generator`** | Generates Riverpod provider boilerplate from `@riverpod` annotations | Requires the developer to write providers manually; no CRUD lifecycle |
| **`go_router_builder`** | Typed `go_router` routes from `@TypedGoRoute` annotations | Routes only; no screens, no state management |
| **`retrofit.dart`** | Typed HTTP client generation | API client only; no UI or state management |
| **OpenAPI Generator (`dart-dio`)** | Full Dart API client from OpenAPI spec | API client only; generated code is not annotation-driven |
| **`mason`** | File scaffolding from Mustache templates | One-shot; not reactive to model changes; requires manual template maintenance |
| **`stacked_generator`** | MVVM boilerplate | Architecture-specific; no domain-model annotation |
| **`quick_actions_cli`** | Basic CRUD scaffold from a model class | Not maintained; no field-level control; no Riverpod/go_router integration |

**Conclusion:** No existing tool combines domain-model annotations, full CRUD UI screens, Riverpod
state management, API client, and go_router routing in a single, cohesive, regeneration-friendly
package. Prefab Flutter fills this gap.

### 2.2 Gap Analysis

The Flutter ecosystem has excellent *building blocks* (Riverpod, go_router, dio, freezed) but no
*assembler*. Developers routinely write the same boilerplate for every entity:
list screen → detail screen → create form → edit form → Riverpod provider → API client → routes.
For an app with 10 entities this is thousands of lines of repetitive code.

Prefab Flutter's value proposition is identical to Prefab Java's: eliminate the repetitive
scaffolding while remaining transparent (the generated code is readable `.g.dart` files the
developer can inspect and, when needed, override).

---

## 3. Design Philosophy

Prefab Flutter inherits the same principles as Prefab Java:

1. **High-level intent, sensible defaults.** A single `@PrefabView` annotation generates a
   complete CRUD UI. Override defaults incrementally with `@ListColumn`, `@FormField`, etc.

2. **Generated code is visible.** All generated code lands in `.prefab.g.dart` files that are
   committed alongside the model. Developers can read, understand, and debug them.

3. **Opt-in override.** When generated code does not fit, the developer writes a manual screen
   and Prefab Flutter skips generation for that artefact (same pattern as Prefab Java's
   `JavaFileWriter` skip-if-exists behaviour).

4. **Convention over configuration.** Field types determine default widget choices. `String` →
   `TextField`, `double` → numeric `TextField`, `bool` → `Switch`, `DateTime` → date picker,
   `enum` → dropdown. Override with `@FormField(widget: FieldWidget.multilineText)`.

5. **Pluggable.** The generator is built around the `PrefabGeneratorBase` extension point so
   teams can add custom generators (e.g. a custom Material 3 design system) without forking.

6. **Backend-aligned by default.** When the backend is also a Prefab Java project, Prefab
   Flutter reads the OpenAPI spec (or the Prefab annotations directly via a shared schema) to
   derive the `@PrefabApi` path, field types, and validation rules automatically.

---

## 4. Proposed Annotation API

All annotations are in the `prefab_flutter_annotations` package.

### 4.1 Entity-level Annotations

#### `@PrefabView`

Marks a Dart class as a Prefab-managed UI entity. Always required.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `title` | `String` | — (required) | App-bar title on the list screen |
| `icon` | `int?` | `Icons.list` | Material icon code-point for navigation drawer |

#### `@PrefabApi`

Configures the REST endpoint.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `path` | `String` | — (required) | Path relative to `dio` base URL. May contain `{param}` placeholders for parent resources |

### 4.2 Operation Annotations

#### `@PrefabCreate`

| Attribute | Type | Default | Description |
|---|---|---|---|
| `title` | `String?` | `'New {Entity}'` | App-bar title on the create screen |
| `submitLabel` | `String` | `'Create'` | Primary button label |

#### `@PrefabUpdate`

| Attribute | Type | Default | Description |
|---|---|---|---|
| `title` | `String?` | `'Edit {Entity}'` | App-bar title on the edit screen |
| `submitLabel` | `String` | `'Save'` | Primary button label |

#### `@PrefabDelete`

| Attribute | Type | Default | Description |
|---|---|---|---|
| `confirmMessage` | `String` | `'Are you sure…'` | Confirmation dialog message |

### 4.3 Field-level Annotations

#### `@ListColumn`

Controls list screen column visibility and behaviour.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | — (required) | Column header |
| `sortable` | `bool` | `false` | Enables sort controls |
| `searchable` | `bool` | `false` | Includes field in server-side search |
| `flex` | `int` | `1` | Relative column width |

#### `@FormField`

Controls create/edit form field appearance and validation.

| Attribute | Type | Default | Description |
|---|---|---|---|
| `label` | `String` | — (required) | Label above the input |
| `hint` | `String?` | `null` | Placeholder text inside the input |
| `validators` | `List<Validator>` | `[]` | Ordered validation rules |
| `widget` | `FieldWidget` | `FieldWidget.auto` | Forces a specific widget type |

#### `@Hidden`

| Attribute | Type | Default | Description |
|---|---|---|---|
| `inList` | `bool` | `true` | Hides from list columns |
| `inForm` | `bool` | `true` | Hides from form fields |

#### `@PrefabParent`

Marks a field as a parent-resource reference. The parent ID is extracted from the route
and injected into API calls (mirrors `@Parent` in Prefab Java).

### 4.4 Complete Example

```dart
// product.dart — what the developer writes (≈ 50 lines)

import 'package:json_annotation/json_annotation.dart';
import 'package:prefab_flutter_annotations/prefab_flutter_annotations.dart';

part 'product.g.dart';
part 'product.prefab.g.dart';

@PrefabView(title: 'Products')
@PrefabCreate(title: 'New Product', submitLabel: 'Create')
@PrefabUpdate(title: 'Edit Product', submitLabel: 'Save')
@PrefabDelete(confirmMessage: 'Remove this product permanently?')
@PrefabApi(path: '/products')
@JsonSerializable()
class Product {
  final String id;

  @ListColumn(label: 'Name', sortable: true, searchable: true)
  @FormField(label: 'Name', hint: 'Enter product name', validators: [Validator.required])
  final String name;

  @ListColumn(label: 'Price (€)', sortable: true)
  @FormField(label: 'Price', hint: '0.00', validators: [Validator.required, Validator.positiveNumber])
  final double price;

  @ListColumn(label: 'Category', searchable: true)
  @FormField(label: 'Category')
  final String category;

  @Hidden()  // present in API responses, never shown in UI
  final String? description;

  const Product({
    required this.id,
    required this.name,
    required this.price,
    required this.category,
    this.description,
  });

  factory Product.fromJson(Map<String, dynamic> json) => _$ProductFromJson(json);
  Map<String, dynamic> toJson() => _$ProductToJson(this);
}
```

Running `flutter pub run build_runner build` generates `product.prefab.g.dart` containing
~400 lines of idiomatic Flutter code (see §5 and the PoC in `flutter/prefab_flutter_example/`).

---

## 5. Generated Artefacts

All artefacts are generated into a single `{model}.prefab.g.dart` `part` file.

### 5.1 List Screen

`{Entity}ListScreen` — a `ConsumerWidget` that:

- Displays an `AppBar` with the `@PrefabView.title`
- Renders a `PrefabSearchBar` if any field has `@ListColumn(searchable: true)`
- Renders a `DataTable` with one `DataColumn` per `@ListColumn` field; sortable columns
  include `onSort` callbacks that call the provider's `sort()` method
- Renders `DataRow`s with `onSelectChanged` → detail navigation
- Renders `onLongPress` on rows to trigger delete confirmation if `@PrefabDelete` is present
- Renders a `FloatingActionButton` → create form if `@PrefabCreate` is present
- Renders a `PrefabPaginationBar` (previous / next page controls)

### 5.2 Detail Screen

`{Entity}DetailScreen` — a `ConsumerWidget` that:

- Loads the entity via `{entity}DetailProvider(id)` from the API
- Renders each non-`@Hidden(inList: true)` field as a read-only `ListTile`
- Provides an "Edit" `IconButton` in the `AppBar` if `@PrefabUpdate` is present
- Provides a "Delete" `IconButton` / menu option if `@PrefabDelete` is present

### 5.3 Create / Edit Form Screens

`{Entity}CreateScreen` and `{Entity}EditScreen` — `ConsumerStatefulWidget`s that:

- Render a `Form` with one input widget per `@FormField`-annotated field
- Select the widget based on the Dart field type (or `@FormField.widget` override)
- Apply `validator` callbacks from `@FormField.validators`
- Pre-populate controllers from the loaded entity (`EditScreen` only)
- Call `provider.create(...)` / `provider.update(id, ...)` on submit
- Show a `SnackBar` on success and `pop()` the screen
- Show a `SnackBar` on failure with the error message

### 5.4 Riverpod Providers

`{Entity}ListNotifier` extends `AsyncNotifier<PrefabPage<{Entity}>>`:

| Method | Behaviour |
|---|---|
| `build()` | Loads page 0 from API |
| `search(String)` | Updates search query, resets to page 0, invalidates |
| `sort(String, bool)` | Updates sort field/direction, invalidates |
| `nextPage()` / `previousPage()` | Increments/decrements page, invalidates |
| `create(...)` | POST via API client, then invalidates |
| `update(id, ...)` | PUT via API client, then invalidates |
| `delete(id)` | DELETE via API client, then invalidates |

`{entity}DetailProvider(String id)` — a simple `FutureProvider` that fetches a single entity.

### 5.5 API Client

`{Entity}ApiClient` — a plain Dart class wrapping `Dio`:

| Method | HTTP | Description |
|---|---|---|
| `list({page, size, search, sortField, sortAscending})` | `GET /path` | Returns `PrefabPage<{Entity}>` |
| `getById(String id)` | `GET /path/{id}` | Returns `{Entity}` |
| `create(Map body)` | `POST /path` | Returns created `{Entity}` |
| `update(String id, Map body)` | `PUT /path/{id}` | Returns updated `{Entity}` |
| `delete(String id)` | `DELETE /path/{id}` | Returns `void` |

A `@riverpod` factory function `{entity}ApiClient(...)` exposes the client as a Riverpod
provider so it can be overridden in widget tests.

### 5.6 go_router Routes

Typed `@TypedGoRoute` classes generated per entity:

| Route class | Path | Target screen |
|---|---|---|
| `{Entity}ListRoute` | `/{entities}` | `{Entity}ListScreen` |
| `{Entity}DetailRoute` | `/{entities}/:id` | `{Entity}DetailScreen` |
| `{Entity}CreateRoute` | `/{entities}/create` | `{Entity}CreateScreen` |
| `{Entity}EditRoute` | `/{entities}/:id/edit` | `{Entity}EditScreen` |

A top-level `$prefabRoutes` list is generated in a dedicated `routes.prefab.g.dart` and
passed to `GoRouter(routes: [...])`.

---

## 6. Build Pipeline

### 6.1 Option A — `source_gen` + `build_runner` Plugin

**How it works:**

```
Developer writes product.dart with @PrefabView annotations
        │
        ▼
flutter pub run build_runner build
        │
        ▼
build_runner invokes PrefabFlutterBuilder
        │
        ▼
source_gen reads annotations via Dart analyzer API
        │
        ▼
Each sub-generator (ListScreenGenerator, FormScreenGenerator, …)
emits Dart source as strings
        │
        ▼
product.prefab.g.dart written to disk alongside product.dart
```

**Pros:**
- Idiomatic Flutter toolchain — no new CLI or IDE plugin needed
- Incremental: only files with changed annotations are regenerated
- Integrates seamlessly with `freezed`, `riverpod_generator`, `go_router_builder`
  in a single `build_runner build` invocation
- Well-documented extension point (`source_gen`, `build`, `analyzer`)

**Cons:**
- `build_runner` can be slow on large projects
- Dart `analyzer` API is verbose to work with
- All generated code lands in `.g.dart` files checked into source control
  (same as `freezed` / `json_serializable` — accepted norm in Flutter community)

### 6.2 Option B — Standalone CLI Tool

A dedicated `prefab_flutter` CLI reads source files and produces `.g.dart` files without
`build_runner`.

**Pros:** Faster, no `build_runner` dependency

**Cons:** Reinvents the wheel; no incremental support out of the box; non-standard workflow;
harder IDE integration

### 6.3 Option C — YAML / JSON Model File

A `.prefab.yaml` model file describes entities; a CLI generates all Dart code.

**Pros:** No Dart annotation parsing needed; model can be shared with the backend

**Cons:** Duplicates domain model; divorced from the Dart type system; refactoring a field name
requires updating both the Dart class and the YAML; unfamiliar workflow for Flutter developers

### 6.4 Recommendation

**Option A (`source_gen` + `build_runner`) is the recommended approach.**

It is the idiomatic Flutter / Dart code-generation mechanism, aligns with the annotation-driven
philosophy of Prefab Java, and composes naturally with other build_runner-based tools the
developer already uses. The `source_gen` API maps closely to Java's `javax.annotation.processing`
API that Prefab Java already uses, which lowers the learning curve for contributors.

---

## 7. Proof of Concept

The PoC is located in `flutter/` at the repository root:

```
flutter/
├── prefab_flutter_annotations/   # Dart annotation definitions
│   ├── pubspec.yaml
│   └── lib/
│       ├── prefab_flutter_annotations.dart
│       └── src/annotations.dart         # @PrefabView, @PrefabCreate, @ListColumn, …
│
├── prefab_flutter/               # build_runner code generator
│   ├── pubspec.yaml
│   ├── build.yaml                # builder registration
│   └── lib/
│       ├── prefab_flutter.dart
│       └── src/
│           ├── builder.dart             # PrefabFlutterGenerator (composite)
│           ├── model/
│           │   └── entity_manifest.dart # EntityManifest, FieldManifest
│           └── generators/
│               ├── prefab_generator_base.dart
│               ├── list_screen_generator.dart
│               ├── form_screen_generator.dart
│               ├── provider_generator.dart
│               ├── api_client_generator.dart
│               └── routes_generator.dart
│
└── prefab_flutter_example/       # Example Flutter app
    ├── pubspec.yaml
    ├── lib/
    │   ├── main.dart
    │   └── src/
    │       ├── config/
    │       │   ├── dio_provider.dart    # Shared Dio provider
    │       │   └── router.dart          # GoRouter wired to $prefabRoutes
    │       └── models/
    │           ├── product.dart          ← developer writes this (49 lines)
    │           └── product.prefab.g.dart ← Prefab Flutter generates this (~400 lines)
```

### 7.2 Developer Input

The developer writes `product.dart` (49 lines). See
`flutter/prefab_flutter_example/lib/src/models/product.dart` for the full source.

### 7.3 Generated Output Summary

From those 49 lines, `product.prefab.g.dart` (~400 lines) contains:

| Artefact | Class / Symbol | Lines (approx.) |
|---|---|---|
| Typed routes | `ProductListRoute`, `ProductDetailRoute`, `ProductCreateRoute`, `ProductEditRoute` | 35 |
| REST API client | `ProductApiClient`, `productApiClientProvider` | 60 |
| Riverpod providers | `ProductListNotifier`, `productDetailProvider` | 80 |
| List screen | `ProductListScreen` | 80 |
| Create form screen | `ProductCreateScreen` | 75 |
| Edit form screen | `ProductEditScreen` | 90 |

**Ratio: ~1 line of annotation-driven model → ~8 lines of generated UI code.**

This is comparable to Prefab Java's ratio and validates the approach.

---

## 8. Generalising to Prefab Frontend

The design decisions made for Prefab Flutter apply almost identically to a framework-agnostic
**Prefab Frontend** layer targeting React, Angular, or Vue:

| Concern | Prefab Flutter | Prefab Frontend (generalised) |
|---|---|---|
| Model definition | Dart class with annotations | TypeScript class / interface with decorators |
| Code generation trigger | `build_runner` plugin | webpack plugin, Vite plugin, or standalone CLI |
| State management | Riverpod `AsyncNotifier` | Redux Toolkit slice / Zustand store / Pinia store |
| Routing | `go_router` typed routes | React Router v6 / Angular Router / Vue Router |
| HTTP client | `dio` | `axios` / Angular `HttpClient` / `fetch` |
| Form validation | Flutter `Form` + `validator` callbacks | React Hook Form / Angular Reactive Forms / VeeValidate |
| Component granularity | Screen → Table → Row | Page → Component → Cell |

**Key insight:** The annotation API (`@PrefabView`, `@ListColumn`, `@FormField`, etc.) is
UI-framework–agnostic. It expresses *intent* (this field appears in lists; this field appears in
forms with this validator). The generator layer translates intent into framework-specific code.

A possible architecture for Prefab Frontend:

```
prefab_frontend_annotations  (TS decorators — shared across frameworks)
    │
    ├── prefab_frontend_react    (webpack/vite plugin, generates React + RTK components)
    ├── prefab_frontend_angular  (schematics plugin, generates Angular components + services)
    └── prefab_frontend_vue      (vite plugin, generates Vue 3 SFCs + Pinia stores)
```

This mirrors how Prefab Java's `prefab-core` (annotations) is separate from `prefab-kafka`,
`prefab-pubsub`, etc. (platform implementations).

---

## 9. Open Questions

1. **Detail screen template.** Should the detail screen use a `ListTile`-per-field layout, a
   custom `Card`, or delegate entirely to the developer? A `@DetailLayout` annotation could
   specify `list | card | custom`.

2. **Nested entities / `@PrefabParent`.** When `OrderLine` has `@PrefabParent` referencing
   `Order`, should the generated `OrderLineListScreen` be embedded inside `OrderDetailScreen`
   as a tab/section, or always a separate navigated route?

3. **Dark mode / theming.** Should generated screens be pure Material 3 (and benefit from the
   app's `ThemeData`) or should Prefab Flutter expose a `PrefabTheme` extension point?

4. **Form field type inference.** The PoC uses `TextFormField` for all types. The full
   implementation needs to select widgets based on the Dart type: `DateTime` → `DatePicker`,
   `enum` → `DropdownButton`, custom enums → same. Should this mapping be in annotations or
   auto-inferred?

5. **Validation beyond built-in `Validator` enum.** Complex cross-field validators (e.g.
   "end date must be after start date") cannot be expressed as a simple enum. A `@Validator`
   annotation accepting a function reference or a `ValidatorClass` approach is needed.

6. **Backend integration / shared schema.** When the backend is Prefab Java, the frontend
   should be able to consume the generated OpenAPI spec to auto-derive `@PrefabApi.path`,
   field types, and server-side validation rules. This eliminates duplication between the
   backend model and the Flutter model.

7. **Offline / optimistic updates.** Should the generated `AsyncNotifier` support optimistic
   updates (updating local state before the API call completes)?

8. **Testing utilities.** Should Prefab Flutter generate widget tests for each generated
   screen, mirroring Prefab Java's generated `TestClient`?

9. **Search and sort input sanitization.** The generated API client passes `search` and
   `sortField` query parameters directly to the server. In production these values come from
   user input (search bar text, sort column name). The full implementation must encode these
   values correctly (percent-encoding via `dio`'s `queryParameters` map handles URL encoding
   automatically) and ensure the `sortField` value is restricted to a known allow-list of
   field names derived from `@ListColumn(sortable: true)` fields — preventing injection of
   arbitrary sort expressions.

---

## 10. Follow-up Tasks

The following backlog tasks are recommended to implement Prefab Flutter after this spike is
accepted:

| Priority | Title | Description |
|---|---|---|
| High | Publish `prefab_flutter_annotations` to pub.dev | Stable public API for the annotations package |
| High | Implement `ListScreenGenerator` fully | Handle responsive layout (mobile/tablet/desktop), `@PrefabParent` nesting, `PrefabPage` model |
| High | Implement `FormScreenGenerator` fully | Type-based widget selection, cross-field validation, file upload support |
| High | Implement `ProviderGenerator` fully | Error boundary, optimistic update option |
| High | Implement `ApiClientGenerator` fully | Authentication headers, error mapping, retry |
| High | Implement `RoutesGenerator` fully | Nested parent routes, named routes |
| Medium | Implement `DetailScreenGenerator` | Detail layout variants (list / card / custom) |
| Medium | Create `PrefabPage<T>` shared model class | Standardise pagination response parsing |
| Medium | Create `PrefabSearchBar`, `PrefabPaginationBar`, `PrefabDeleteDialog` shared widgets | Runtime widget library (separate package: `prefab_flutter_widgets`) |
| Medium | Backend integration — read Prefab Java OpenAPI spec | Derive `@PrefabApi.path`, field types, and validators from the backend spec automatically |
| Medium | Write widget tests for all generated screen types | Ensure generated code is always correct |
| Low | Support `@PrefabParent` nested entity navigation | Embed nested list in detail screen as a tab |
| Low | Custom `PrefabTheme` extension point | Allow design-system overrides |
| Low | Explore Prefab Frontend (React) | Apply same annotation API to generate React + RTK + React Router components |
| Low | Explore Prefab Frontend (Angular) | Apply same annotation API to generate Angular components + services + router |
