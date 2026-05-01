import 'package:analyzer/dart/element/element.dart';
import 'package:source_gen/source_gen.dart';
import 'package:prefab_flutter_annotations/prefab_flutter_annotations.dart';

/// Parsed representation of a single `@PrefabView`-annotated class.
///
/// All generators receive an [EntityManifest] so they never need to touch
/// the analyser API directly.
class EntityManifest {
  final String className;
  final String title;
  final String? apiPath;
  final bool hasCreate;
  final bool hasUpdate;
  final bool hasDelete;
  final String createTitle;
  final String updateTitle;
  final String createSubmitLabel;
  final String updateSubmitLabel;
  final String deleteConfirmMessage;
  final List<FieldManifest> fields;

  const EntityManifest({
    required this.className,
    required this.title,
    required this.apiPath,
    required this.hasCreate,
    required this.hasUpdate,
    required this.hasDelete,
    required this.createTitle,
    required this.updateTitle,
    required this.createSubmitLabel,
    required this.updateSubmitLabel,
    required this.deleteConfirmMessage,
    required this.fields,
  });

  /// Lowercased entity name used as a prefix for generated class names and
  /// file names (e.g. `product` → `ProductListScreen`).
  String get entityName => className[0].toLowerCase() + className.substring(1);

  /// Fields visible in the list screen (carry `@ListColumn`).
  List<FieldManifest> get listFields =>
      fields.where((f) => f.listColumn != null).toList();

  /// Fields visible in the form screens (carry `@FormField`).
  List<FieldManifest> get formFields =>
      fields.where((f) => f.formField != null).toList();

  factory EntityManifest.fromElement(
    ClassElement element,
    ConstantReader annotation,
  ) {
    final title = annotation.read('title').stringValue;

    final createChecker = TypeChecker.fromRuntime(PrefabCreate);
    final updateChecker = TypeChecker.fromRuntime(PrefabUpdate);
    final deleteChecker = TypeChecker.fromRuntime(PrefabDelete);
    final apiChecker = TypeChecker.fromRuntime(PrefabApi);

    final createAnnotation = createChecker.firstAnnotationOf(element);
    final updateAnnotation = updateChecker.firstAnnotationOf(element);
    final deleteAnnotation = deleteChecker.firstAnnotationOf(element);
    final apiAnnotation = apiChecker.firstAnnotationOf(element);

    final hasCreate = createAnnotation != null;
    final hasUpdate = updateAnnotation != null;
    final hasDelete = deleteAnnotation != null;

    final createReader = hasCreate ? ConstantReader(createAnnotation) : null;
    final updateReader = hasUpdate ? ConstantReader(updateAnnotation) : null;
    final deleteReader = hasDelete ? ConstantReader(deleteAnnotation) : null;
    final apiReader = apiAnnotation != null ? ConstantReader(apiAnnotation) : null;

    final fields = element.fields.map(FieldManifest.fromElement).toList();

    return EntityManifest(
      className: element.name,
      title: title,
      apiPath: apiReader?.read('path').stringValue,
      hasCreate: hasCreate,
      hasUpdate: hasUpdate,
      hasDelete: hasDelete,
      createTitle: createReader?.peek('title')?.stringValue ?? 'New ${element.name}',
      updateTitle: updateReader?.peek('title')?.stringValue ?? 'Edit ${element.name}',
      createSubmitLabel: createReader?.read('submitLabel').stringValue ?? 'Create',
      updateSubmitLabel: updateReader?.read('submitLabel').stringValue ?? 'Save',
      deleteConfirmMessage: deleteReader?.read('confirmMessage').stringValue ??
          'Are you sure you want to delete this item?',
      fields: fields,
    );
  }
}

/// Parsed representation of a single field on a `@PrefabView` class.
class FieldManifest {
  final String name;
  final String dartType;
  final ListColumnManifest? listColumn;
  final FormFieldManifest? formField;
  final bool isHiddenInList;
  final bool isHiddenInForm;
  final bool isParent;

  const FieldManifest({
    required this.name,
    required this.dartType,
    required this.listColumn,
    required this.formField,
    required this.isHiddenInList,
    required this.isHiddenInForm,
    required this.isParent,
  });

  factory FieldManifest.fromElement(FieldElement element) {
    final listColumnChecker = TypeChecker.fromRuntime(ListColumn);
    final formFieldChecker = TypeChecker.fromRuntime(FormField);
    final hiddenChecker = TypeChecker.fromRuntime(Hidden);
    final parentChecker = TypeChecker.fromRuntime(PrefabParent);

    final listColumnAnnotation = listColumnChecker.firstAnnotationOf(element);
    final formFieldAnnotation = formFieldChecker.firstAnnotationOf(element);
    final hiddenAnnotation = hiddenChecker.firstAnnotationOf(element);

    ListColumnManifest? listColumn;
    if (listColumnAnnotation != null) {
      final r = ConstantReader(listColumnAnnotation);
      listColumn = ListColumnManifest(
        label: r.read('label').stringValue,
        sortable: r.read('sortable').boolValue,
        searchable: r.read('searchable').boolValue,
        flex: r.read('flex').intValue,
      );
    }

    FormFieldManifest? formField;
    if (formFieldAnnotation != null) {
      final r = ConstantReader(formFieldAnnotation);
      formField = FormFieldManifest(
        label: r.read('label').stringValue,
        hint: r.peek('hint')?.stringValue,
      );
    }

    bool isHiddenInList = false;
    bool isHiddenInForm = false;
    if (hiddenAnnotation != null) {
      final r = ConstantReader(hiddenAnnotation);
      isHiddenInList = r.read('inList').boolValue;
      isHiddenInForm = r.read('inForm').boolValue;
    }

    return FieldManifest(
      name: element.name,
      dartType: element.type.getDisplayString(withNullability: false),
      listColumn: listColumn,
      formField: formField,
      isHiddenInList: isHiddenInList,
      isHiddenInForm: isHiddenInForm,
      isParent: parentChecker.hasAnnotationOf(element),
    );
  }
}

class ListColumnManifest {
  final String label;
  final bool sortable;
  final bool searchable;
  final int flex;

  const ListColumnManifest({
    required this.label,
    required this.sortable,
    required this.searchable,
    required this.flex,
  });
}

class FormFieldManifest {
  final String label;
  final String? hint;

  const FormFieldManifest({required this.label, this.hint});
}
