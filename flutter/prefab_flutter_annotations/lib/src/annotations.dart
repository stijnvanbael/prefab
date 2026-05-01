// ---------------------------------------------------------------------------
// Entity-level annotations
// ---------------------------------------------------------------------------

/// Marks a Dart class as a Prefab-managed UI entity.
///
/// Prefab Flutter generates a list screen, a detail screen, and the associated
/// Riverpod state-management layer and go_router routes for every class
/// carrying this annotation.
///
/// The [title] is used as the app-bar title on the list screen. Override
/// [icon] with any Material icon code-point to customise the navigation icon.
///
/// Example:
/// ```dart
/// @View(title: 'Products', icon: Icons.inventory_2)
/// @Api(path: '/products')
/// class Product { ... }
/// ```
class View {
  /// Human-readable title shown in the app-bar and navigation drawer.
  final String title;

  /// Optional Material icon used in the navigation drawer entry.
  /// Defaults to [Icons.list] when not provided.
  final int? icon;

  const View({required this.title, this.icon});
}

// ---------------------------------------------------------------------------
// CRUD operation annotations
// ---------------------------------------------------------------------------

/// Generates a create-form screen and wires it to a POST endpoint.
///
/// The annotated class must also carry [Api] so the generator knows
/// the REST endpoint to call. All fields annotated with [FormField] appear
/// in the generated form; remaining fields are hidden.
///
/// Example:
/// ```dart
/// @View(title: 'Products')
/// @Create(title: 'New Product', submitLabel: 'Create')
/// @Api(path: '/products')
/// class Product { ... }
/// ```
class Create {
  /// App-bar title of the create-form screen. Defaults to `'New <EntityName>'`.
  final String? title;

  /// Label for the primary submit button. Defaults to `'Create'`.
  final String submitLabel;

  const Create({this.title, this.submitLabel = 'Create'});
}

/// Generates an edit-form screen and wires it to a PUT endpoint.
///
/// Shares the same form-field definitions as [Create]. The generated
/// form is pre-populated with the current aggregate values.
///
/// Example:
/// ```dart
/// @Update(title: 'Edit Product', submitLabel: 'Save')
/// class Product { ... }
/// ```
class Update {
  /// App-bar title of the edit-form screen. Defaults to `'Edit <EntityName>'`.
  final String? title;

  /// Label for the primary submit button. Defaults to `'Save'`.
  final String submitLabel;

  const Update({this.title, this.submitLabel = 'Save'});
}

/// Generates a delete confirmation dialog and wires it to a DELETE endpoint.
///
/// Example:
/// ```dart
/// @Delete(confirmMessage: 'Remove this product?')
/// class Product { ... }
/// ```
class Delete {
  /// Confirmation message shown in the dialog. Defaults to
  /// `'Are you sure you want to delete this item?'`.
  final String confirmMessage;

  const Delete({
    this.confirmMessage = 'Are you sure you want to delete this item?',
  });
}

// ---------------------------------------------------------------------------
// API configuration annotation
// ---------------------------------------------------------------------------

/// Configures the REST API endpoint used by the generated screens and providers.
///
/// [path] is appended to the base URL configured in `PrefabConfig`. The
/// generator produces a Riverpod `AsyncNotifier` that performs standard
/// list / get-by-id / create / update / delete calls via the `dio` HTTP client.
///
/// Example:
/// ```dart
/// @Api(path: '/products')
/// class Product { ... }
///
/// // With a parent resource:
/// @Api(path: '/orders/{orderId}/lines')
/// class OrderLine { ... }
/// ```
class Api {
  /// Path relative to the configured base URL. May contain `{paramName}`
  /// placeholders for parent-resource identifiers.
  final String path;

  const Api({required this.path});
}

// ---------------------------------------------------------------------------
// Field-level annotations
// ---------------------------------------------------------------------------

/// Controls how a field appears in the generated list screen.
///
/// Fields without this annotation are not shown in the list. At least one
/// field per entity should carry [ListColumn].
///
/// Example:
/// ```dart
/// @ListColumn(label: 'Product Name', sortable: true, searchable: true)
/// final String name;
/// ```
class ListColumn {
  /// Column header label.
  final String label;

  /// Whether this column can be sorted by the user. Defaults to `false`.
  final bool sortable;

  /// Whether this field is included in the search filter. Defaults to `false`.
  final bool searchable;

  /// Optional display width hint (Flutter flex factor). Defaults to 1.
  final int flex;

  const ListColumn({
    required this.label,
    this.sortable = false,
    this.searchable = false,
    this.flex = 1,
  });
}

/// Controls how a field appears in the generated create/edit form screens.
///
/// Fields without this annotation are omitted from forms. Use [Hidden] to
/// explicitly exclude a field from both lists and forms.
///
/// Example:
/// ```dart
/// @FormField(
///   label: 'Price',
///   hint: 'Enter price in EUR',
///   validators: [Validator.required, Validator.positiveNumber],
/// )
/// final double price;
/// ```
class FormField {
  /// Field label shown above the input widget.
  final String label;

  /// Placeholder text inside the input widget.
  final String? hint;

  /// Ordered list of validator rules applied on form submit.
  final List<Validator> validators;

  /// Type hint that overrides Prefab's default widget selection.
  /// Use [FieldWidget.auto] (default) to let Prefab pick based on the
  /// Dart field type.
  final FieldWidget widget;

  const FormField({
    required this.label,
    this.hint,
    this.validators = const [],
    this.widget = FieldWidget.auto,
  });
}

/// Explicitly hides a field from the generated list and/or form screens.
///
/// The field is still serialised/deserialised from the API response.
///
/// Example:
/// ```dart
/// @Hidden()                      // hidden from both list and forms
/// @Hidden(inList: true)          // hidden from list only
/// @Hidden(inForm: true)          // hidden from forms only
/// final String internalCode;
/// ```
class Hidden {
  /// Whether the field is hidden in list screens. Defaults to `true`.
  final bool inList;

  /// Whether the field is hidden in form screens. Defaults to `true`.
  final bool inForm;

  const Hidden({this.inList = true, this.inForm = true});
}

/// Marks a field as referencing a parent resource.
///
/// When a field carries [Parent], the list and detail screens for this
/// entity are nested under the parent's route (e.g. `/orders/:orderId/lines`).
/// The parent ID is extracted from the route and injected into API calls
/// automatically.
///
/// Example:
/// ```dart
/// @Parent()
/// final String orderId;
/// ```
class Parent {
  const Parent();
}

// ---------------------------------------------------------------------------
// Supporting enums
// ---------------------------------------------------------------------------

/// Built-in validation rules used with [FormField.validators].
enum Validator {
  /// Field must not be empty or null.
  required,

  /// Field must contain a valid e-mail address.
  email,

  /// Field must be a positive number.
  positiveNumber,

  /// Field must match URL format.
  url,
}

/// Form-widget type hint used with [FormField.widget].
enum FieldWidget {
  /// Prefab picks the widget based on the field's Dart type:
  /// - `String` → [TextField]
  /// - `int` / `double` → [TextField] with numeric keyboard
  /// - `bool` → [Switch]
  /// - `DateTime` → [DatePicker]
  /// - `enum` → [DropdownButton]
  auto,

  /// Forces a multi-line [TextField].
  multilineText,

  /// Forces a password [TextField] (obscured input).
  password,

  /// Forces a date-picker.
  datePicker,

  /// Forces a dropdown selector.
  dropdown,

  /// Forces a toggle/switch.
  toggle,
}
