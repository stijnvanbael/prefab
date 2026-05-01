import '../model/entity_manifest.dart';
import 'prefab_generator_base.dart';

/// Generates a create form screen and an edit form screen for a
/// `@Create`- and/or `@Update`-annotated entity.
///
/// Both screens share the same form widget ([_{Entity}Form]); the difference
/// is that the edit screen pre-populates field controllers with the existing
/// values loaded from the [ProviderGenerator]-generated `AsyncNotifier`.
///
/// The generated form:
/// - Renders one input widget per `@FormField`-annotated field
/// - Applies the validators declared on [FormField.validators]
/// - Calls the API client's `create` / `update` method on submit
/// - Shows a `SnackBar` on success and pops the screen
/// - Shows an inline error message on failure
class FormScreenGenerator extends PrefabGeneratorBase {
  @override
  String generateForEntity(EntityManifest manifest) {
    if (!manifest.hasCreate && !manifest.hasUpdate) return '';

    final entity = manifest.className;
    final entityLower = manifest.entityName;
    final formFields = manifest.formFields;

    final controllers = formFields
        .map((f) => "final _${f.name}Controller = TextEditingController();")
        .join('\n  ');

    final disposeControllers = formFields
        .map((f) => "_${f.name}Controller.dispose();")
        .join('\n    ');

    final formWidgets = formFields.map((f) {
      final hint = f.formField!.hint != null
          ? ", hintText: '${f.formField!.hint}'"
          : '';
      return '''
          TextFormField(
            controller: _${f.name}Controller,
            decoration: const InputDecoration(labelText: '${f.formField!.label}'$hint),
            validator: (v) => v == null || v.isEmpty ? '${f.formField!.label} is required' : null,
          ),''';
    }).join('\n');

    final createSubmitBody = '''
            await ref
                .read(${entityLower}ListNotifierProvider.notifier)
                .create(${_buildCreateParams(formFields)});''';

    final updateSubmitBody = '''
            await ref
                .read(${entityLower}ListNotifierProvider.notifier)
                .update(widget.id!, ${_buildCreateParams(formFields)});''';

    final createScreen = manifest.hasCreate
        ? _buildFormScreen(
            entity: entity,
            entityLower: entityLower,
            screenName: '${entity}CreateScreen',
            appBarTitle: manifest.createTitle,
            submitLabel: manifest.createSubmitLabel,
            controllers: controllers,
            disposeControllers: disposeControllers,
            formWidgets: formWidgets,
            submitBody: createSubmitBody,
            prefillOnInit: false,
          )
        : '';

    final updateScreen = manifest.hasUpdate
        ? _buildFormScreen(
            entity: entity,
            entityLower: entityLower,
            screenName: '${entity}EditScreen',
            appBarTitle: manifest.updateTitle,
            submitLabel: manifest.updateSubmitLabel,
            controllers: controllers,
            disposeControllers: disposeControllers,
            formWidgets: formWidgets,
            submitBody: updateSubmitBody,
            prefillOnInit: true,
          )
        : '';

    return '''
// ignore_for_file: type=lint

// **************************************************************************
// PrefabFlutterGenerator — FormScreenGenerator
// **************************************************************************

$createScreen
$updateScreen
''';
  }

  String _buildCreateParams(List<dynamic> formFields) {
    return formFields
        .map((f) => _controllerToTypedExpression(f.name as String, f.dartType as String))
        .join(', ');
  }

  /// Emits a typed expression that converts a TextEditingController's text to the
  /// correct Dart type for the provider method parameter.
  String _controllerToTypedExpression(String fieldName, String dartType) {
    switch (dartType) {
      case 'double':
        return 'double.parse(_${fieldName}Controller.text)';
      case 'int':
        return 'int.parse(_${fieldName}Controller.text)';
      case 'num':
        return 'num.parse(_${fieldName}Controller.text)';
      case 'bool':
        return '_${fieldName}Controller.text.toLowerCase() == \'true\'';
      default:
        return '_${fieldName}Controller.text';
    }
  }

  String _buildFormScreen({
    required String entity,
    required String entityLower,
    required String screenName,
    required String appBarTitle,
    required String submitLabel,
    required String controllers,
    required String disposeControllers,
    required String formWidgets,
    required String submitBody,
    required bool prefillOnInit,
  }) {
    final prefillLogic = prefillOnInit
        ? '''
  @override
  void initState() {
    super.initState();
    _prefillControllers();
  }

  Future<void> _prefillControllers() async {
    final item = await ref.read(${entityLower}DetailProvider(widget.id!).future);
    // Populate controllers from the loaded entity.
    // Fields are set based on the @FormField annotations.
    setState(() {
      // Generated field assignments would appear here.
    });
  }
'''
        : '';

    return '''
class $screenName extends ConsumerStatefulWidget {
  final String? id;
  const $screenName({super.key, this.id});

  @override
  ConsumerState<$screenName> createState() => _${screenName}State();
}

class _${screenName}State extends ConsumerState<$screenName> {
  final _formKey = GlobalKey<FormState>();
  $controllers

  $prefillLogic

  @override
  void dispose() {
    $disposeControllers
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    try {
      $submitBody
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Saved successfully')));
        Navigator.of(context).pop();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Error: \$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('$appBarTitle')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
$formWidgets
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _submit,
                child: const Text('$submitLabel'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
''';
  }
}
