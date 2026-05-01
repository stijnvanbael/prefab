import '../model/entity_manifest.dart';
import 'prefab_generator_base.dart';

/// Generates a `dio`-based REST API client for a `@View` + `@Api`
/// entity.
///
/// The generated client class (`{Entity}ApiClient`) is a Riverpod-injectable
/// service that wraps standard HTTP calls:
///
/// - `list(...)` — `GET {path}?page=&size=&search=&sort=`
/// - `getById(String id)` — `GET {path}/{id}`
/// - `create(Map body)` — `POST {path}`
/// - `update(String id, Map body)` — `PUT {path}/{id}`
/// - `delete(String id)` — `DELETE {path}/{id}`
///
/// The `{Entity}ApiClient` is exposed as a Riverpod provider
/// (`{entity}ApiClientProvider`) so it can be overridden in tests.
class ApiClientGenerator extends PrefabGeneratorBase {
  @override
  String generateForEntity(EntityManifest manifest) {
    if (manifest.apiPath == null) return '';

    final entity = manifest.className;
    final entityLower = manifest.entityName;
    final path = manifest.apiPath;

    final createMethod = manifest.hasCreate
        ? '''
  Future<$entity> create(Map<String, dynamic> body) async {
    final response = await _dio.post(_path, data: body);
    return $entity.fromJson(response.data as Map<String, dynamic>);
  }
'''
        : '';

    final updateMethod = manifest.hasUpdate
        ? '''
  Future<$entity> update(String id, Map<String, dynamic> body) async {
    final response = await _dio.put('\$_path/\$id', data: body);
    return $entity.fromJson(response.data as Map<String, dynamic>);
  }
'''
        : '';

    final deleteMethod = manifest.hasDelete
        ? '''
  Future<void> delete(String id) async {
    await _dio.delete('\$_path/\$id');
  }
'''
        : '';

    return '''
// ignore_for_file: type=lint

// **************************************************************************
// PrefabFlutterGenerator — ApiClientGenerator
// **************************************************************************

class ${entity}ApiClient {
  final Dio _dio;
  final String _path = '$path';

  const ${entity}ApiClient(this._dio);

  Future<PrefabPage<$entity>> list({
    int page = 0,
    int size = 20,
    String search = '',
    String sortField = '',
    bool sortAscending = true,
  }) async {
    final response = await _dio.get(
      _path,
      queryParameters: {
        'page': page,
        'size': size,
        if (search.isNotEmpty) 'search': search,
        if (sortField.isNotEmpty) 'sort': '\$sortField,\${sortAscending ? 'asc' : 'desc'}',
      },
    );
    return PrefabPage.fromJson(
      response.data as Map<String, dynamic>,
      (json) => $entity.fromJson(json as Map<String, dynamic>),
    );
  }

  Future<$entity> getById(String id) async {
    final response = await _dio.get('\$_path/\$id');
    return $entity.fromJson(response.data as Map<String, dynamic>);
  }

  $createMethod
  $updateMethod
  $deleteMethod
}

@riverpod
${entity}ApiClient ${entityLower}ApiClient(${entity}ApiClientRef ref) {
  return ${entity}ApiClient(ref.watch(dioProvider));
}
''';
  }
}
