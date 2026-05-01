import 'package:analyzer/dart/element/element.dart';
import 'package:source_gen/source_gen.dart';
import 'package:prefab_flutter_annotations/prefab_flutter_annotations.dart';

import '../model/entity_manifest.dart';

/// Shared base for all Prefab Flutter generators.
///
/// Subclasses override [generateForEntity] to emit source code for a single
/// annotated entity. This class handles annotation discovery and manifest
/// construction.
abstract class PrefabGeneratorBase extends Generator {
  static const _viewChecker = TypeChecker.fromRuntime(PrefabView);

  @override
  Future<String?> generate(LibraryReader library, BuildStep buildStep) async {
    final buffer = StringBuffer();

    for (final annotated in library.annotatedWith(_viewChecker)) {
      final element = annotated.element;
      if (element is! ClassElement) continue;

      final manifest = EntityManifest.fromElement(element, annotated.annotation);
      final output = generateForEntity(manifest);
      if (output.isNotEmpty) {
        buffer.writeln(output);
      }
    }

    return buffer.isEmpty ? null : buffer.toString();
  }

  /// Subclasses emit source code for a single [EntityManifest].
  String generateForEntity(EntityManifest manifest);
}
