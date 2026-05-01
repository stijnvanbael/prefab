import '../model/entity_manifest.dart';
import 'prefab_generator_base.dart';

/// Generates typed `go_router` routes for a `@View` entity.
///
/// The generated route classes follow the `@TypedGoRoute` pattern from
/// `go_router_builder`. Generated routes:
///
/// - `{Entity}ListRoute` — navigates to the list screen (`/{entities}`)
/// - `{Entity}DetailRoute` — navigates to the detail screen (`/{entities}/:id`)
/// - `{Entity}CreateRoute` — navigates to the create form (when `@Create` present)
/// - `{Entity}EditRoute` — navigates to the edit form (when `@Update` present)
///
/// All routes are collected in the top-level `\$prefabRoutes` list, which is
/// passed to `GoRouter(routes: [...])` in your app.
class RoutesGenerator extends PrefabGeneratorBase {
  @override
  String generateForEntity(EntityManifest manifest) {
    final entity = manifest.className;

    // Derive a URL-safe path segment: ProductCategory → product-categories
    final pathSegment = _toKebabPlural(entity);

    final createRouteClass = manifest.hasCreate
        ? '''
class ${entity}CreateRoute extends GoRouteData {
  const ${entity}CreateRoute();

  @override
  Widget build(BuildContext context, GoRouterState state) =>
      const ${entity}CreateScreen();
}
'''
        : '';

    final editRouteClass = manifest.hasUpdate
        ? '''
class ${entity}EditRoute extends GoRouteData {
  final String id;
  const ${entity}EditRoute({required this.id});

  @override
  Widget build(BuildContext context, GoRouterState state) =>
      ${entity}EditScreen(id: id);
}
'''
        : '';

    final subRoutes = [
      if (manifest.hasCreate) 'TypedGoRoute<${entity}CreateRoute>(path: \'create\')',
      if (manifest.hasUpdate) 'TypedGoRoute<${entity}EditRoute>(path: \':id/edit\')',
    ].join(',\n    ');

    final routeDeclaration = subRoutes.isNotEmpty
        ? '''
@TypedGoRoute<${entity}ListRoute>(
  path: '/$pathSegment',
  routes: [
    TypedGoRoute<${entity}DetailRoute>(path: ':id'),
    $subRoutes,
  ],
)
'''
        : '''
@TypedGoRoute<${entity}ListRoute>(
  path: '/$pathSegment',
  routes: [
    TypedGoRoute<${entity}DetailRoute>(path: ':id'),
  ],
)
''';

    return '''
// ignore_for_file: type=lint

// **************************************************************************
// PrefabFlutterGenerator — RoutesGenerator
// **************************************************************************

$routeDeclaration
class ${entity}ListRoute extends GoRouteData {
  const ${entity}ListRoute();

  @override
  Widget build(BuildContext context, GoRouterState state) =>
      const ${entity}ListScreen();
}

class ${entity}DetailRoute extends GoRouteData {
  final String id;
  const ${entity}DetailRoute({required this.id});

  @override
  Widget build(BuildContext context, GoRouterState state) =>
      ${entity}DetailScreen(id: id);
}

$createRouteClass
$editRouteClass
''';
  }

  /// Converts `ProductCategory` → `product-categories`.
  String _toKebabPlural(String className) {
    final kebab = className
        .replaceAllMapped(
          RegExp(r'(?<=[a-z])[A-Z]'),
          (m) => '-${m.group(0)!.toLowerCase()}',
        )
        .toLowerCase();
    // Naive pluralisation: append 's' if not already ending in 's'.
    return kebab.endsWith('s') ? kebab : '${kebab}s';
  }
}
