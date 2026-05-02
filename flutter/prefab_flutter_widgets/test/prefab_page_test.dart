import 'package:flutter_test/flutter_test.dart';
import 'package:prefab_flutter_widgets/prefab_flutter_widgets.dart';

void main() {
  group('PrefabPage', () {
    test('totalPages is zero when pageSize is zero', () {
      final page = PrefabPage<String>(
        items: [],
        totalItems: 100,
        pageSize: 0,
        currentPage: 0,
      );
      expect(page.totalPages, 0);
    });

    test('totalPages rounds up', () {
      final page = PrefabPage<int>(
        items: List.generate(10, (i) => i),
        totalItems: 25,
        pageSize: 10,
        currentPage: 0,
      );
      expect(page.totalPages, 3);
    });

    test('isEmpty is true when items list is empty', () {
      final page = PrefabPage<String>(
        items: [],
        totalItems: 0,
        pageSize: 20,
        currentPage: 0,
      );
      expect(page.isEmpty, isTrue);
    });

    test('isEmpty is false when items list is non-empty', () {
      final page = PrefabPage<String>(
        items: ['a', 'b'],
        totalItems: 2,
        pageSize: 20,
        currentPage: 0,
      );
      expect(page.isEmpty, isFalse);
    });

    test('copyWith replaces only provided fields', () {
      const original = PrefabPage<int>(
        items: [1, 2, 3],
        totalItems: 3,
        pageSize: 10,
        currentPage: 0,
      );
      final copy = original.copyWith(currentPage: 1, totalItems: 30);
      expect(copy.currentPage, 1);
      expect(copy.totalItems, 30);
      expect(copy.pageSize, original.pageSize);
      expect(copy.items, original.items);
    });

    test('equality holds for identical values', () {
      const a = PrefabPage<int>(
        items: [1, 2],
        totalItems: 2,
        pageSize: 10,
        currentPage: 0,
      );
      const b = PrefabPage<int>(
        items: [1, 2],
        totalItems: 2,
        pageSize: 10,
        currentPage: 0,
      );
      expect(a, equals(b));
    });
  });
}
