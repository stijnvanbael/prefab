/// Prefab Flutter code generator.
///
/// This package is a [build_runner] plugin that reads classes annotated with
/// `@PrefabView`, `@PrefabCreate`, `@PrefabUpdate`, `@PrefabDelete`, and
/// `@PrefabApi` and generates:
///
/// - A list screen widget (`{entity}_list_screen.g.dart`)
/// - A detail screen widget (`{entity}_detail_screen.g.dart`)
/// - A create/edit form screen widget (`{entity}_form_screen.g.dart`)
/// - Riverpod `AsyncNotifier` providers (`{entity}_provider.g.dart`)
/// - A `dio`-based API client (`{entity}_api_client.g.dart`)
/// - Typed `go_router` routes (`{entity}_routes.g.dart`)
///
/// Add this package as a `dev_dependency` in your app's `pubspec.yaml` and
/// run `flutter pub run build_runner build` to trigger generation.
library prefab_flutter;

export 'src/builder.dart';
