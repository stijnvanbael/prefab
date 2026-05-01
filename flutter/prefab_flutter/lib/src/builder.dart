import 'package:build/build.dart';
import 'package:source_gen/source_gen.dart';

import 'generators/list_screen_generator.dart';
import 'generators/form_screen_generator.dart';
import 'generators/provider_generator.dart';
import 'generators/api_client_generator.dart';
import 'generators/routes_generator.dart';

/// Entry point registered in `build.yaml`.
///
/// Returns a [LibraryBuilder] that applies all Prefab Flutter generators to
/// every `.dart` file in the target package. Each generator emits a separate
/// `part` file:
///
/// - `{file}.list_screen.g.dart`
/// - `{file}.form_screen.g.dart`
/// - `{file}.provider.g.dart`
/// - `{file}.api_client.g.dart`
/// - `{file}.routes.g.dart`
Builder prefabFlutterBuilder(BuilderOptions options) =>
    LibraryBuilder(
      PrefabFlutterGenerator(options),
      generatedExtension: '.prefab.g.dart',
    );

/// Composite generator that delegates to each specialised sub-generator.
class PrefabFlutterGenerator extends Generator {
  final BuilderOptions options;

  const PrefabFlutterGenerator(this.options);

  @override
  Future<String> generate(LibraryReader library, BuildStep buildStep) async {
    final buffer = StringBuffer();

    for (final generator in _generators) {
      final output = await generator.generate(library, buildStep);
      if (output != null && output.isNotEmpty) {
        buffer.writeln(output);
      }
    }

    return buffer.toString();
  }

  List<Generator> get _generators => [
        ListScreenGenerator(),
        FormScreenGenerator(),
        ProviderGenerator(),
        ApiClientGenerator(),
        RoutesGenerator(),
      ];
}
