import '../model/entity_manifest.dart';
import 'prefab_generator_base.dart';

/// Generates Riverpod `AsyncNotifier` providers for a `@PrefabView` entity.
///
/// The generated provider manages the full CRUD lifecycle:
///
/// - `build()` — loads the first page from the API
/// - `search(String query)` — updates the search filter and reloads
/// - `sort(String field, bool ascending)` — updates the sort order and reloads
/// - `nextPage()` / `previousPage()` — paginates through results
/// - `create(...)` — calls the API client's create method and refreshes
/// - `update(String id, ...)` — calls the API client's update method and refreshes
/// - `delete(String id)` — calls the API client's delete method and refreshes
///
/// A separate `{Entity}DetailProvider` is also generated for the detail and
/// edit form screens.
class ProviderGenerator extends PrefabGeneratorBase {
  @override
  String generateForEntity(EntityManifest manifest) {
    final entity = manifest.className;
    final entityLower = manifest.entityName;
    final formFields = manifest.formFields;

    final createParams = formFields.map((f) => '${f.dartType} ${f.name}').join(', ');
    final updateParams = ['String id', ...formFields.map((f) => '${f.dartType} ${f.name}')].join(', ');
    final createBody = formFields.map((f) => "'${f.name}': ${f.name}").join(', ');
    final updateBody = formFields.map((f) => "'${f.name}': ${f.name}").join(', ');

    final createMethod = manifest.hasCreate
        ? '''
  Future<void> create($createParams) async {
    await _client.create({$createBody});
    ref.invalidateSelf();
    await future;
  }
'''
        : '';

    final updateMethod = manifest.hasUpdate
        ? '''
  Future<void> update($updateParams) async {
    await _client.update(id, {$updateBody});
    ref.invalidateSelf();
    await future;
  }
'''
        : '';

    final deleteMethod = manifest.hasDelete
        ? '''
  Future<void> delete(String id) async {
    await _client.delete(id);
    ref.invalidateSelf();
    await future;
  }
'''
        : '';

    return '''
// ignore_for_file: type=lint

// **************************************************************************
// PrefabFlutterGenerator — ProviderGenerator
// **************************************************************************

@riverpod
class ${entity}ListNotifier extends _\$${entity}ListNotifier {
  late final ${entity}ApiClient _client;
  String _searchQuery = '';
  String _sortField = '';
  bool _sortAscending = true;
  int _page = 0;

  @override
  Future<PrefabPage<$entity>> build() async {
    _client = ref.watch(${entityLower}ApiClientProvider);
    return _client.list(
      page: _page,
      search: _searchQuery,
      sortField: _sortField,
      sortAscending: _sortAscending,
    );
  }

  void search(String query) {
    _searchQuery = query;
    _page = 0;
    ref.invalidateSelf();
  }

  /// Sortable field names, derived from @ListColumn(sortable: true) annotations.
  /// Only these values are accepted by [sort] to prevent arbitrary sort injection.
  static const _allowedSortFields = {${manifest.listFields.where((f) => f.listColumn!.sortable).map((f) => "'${f.name}'").join(', ')}};

  void sort(String field, bool ascending) {
    if (!_allowedSortFields.contains(field)) return;
    _sortField = field;
    _sortAscending = ascending;
    ref.invalidateSelf();
  }

  void nextPage() {
    _page++;
    ref.invalidateSelf();
  }

  void previousPage() {
    if (_page > 0) {
      _page--;
      ref.invalidateSelf();
    }
  }

  $createMethod
  $updateMethod
  $deleteMethod
}

@riverpod
Future<$entity> ${entityLower}Detail(${entity}DetailRef ref, String id) {
  final client = ref.watch(${entityLower}ApiClientProvider);
  return client.getById(id);
}
''';
  }
}
