import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:prefab_flutter_widgets/prefab_flutter_widgets.dart';

enum _Field { name, price }

Widget _wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

void main() {
  group('PrefabSortMenu', () {
    final options = [
      const SortOption<_Field>(label: 'Name', value: _Field.name),
      const SortOption<_Field>(label: 'Price', value: _Field.price),
    ];

    testWidgets('renders sort icon button', (tester) async {
      await tester.pumpWidget(
        _wrap(
          PrefabSortMenu<_Field>(
            options: options,
            onSelected: (_, __) {},
          ),
        ),
      );
      expect(find.byIcon(Icons.sort), findsOneWidget);
    });

    testWidgets('shows all options in popup menu', (tester) async {
      await tester.pumpWidget(
        _wrap(
          PrefabSortMenu<_Field>(
            options: options,
            onSelected: (_, __) {},
          ),
        ),
      );
      await tester.tap(find.byIcon(Icons.sort));
      await tester.pumpAndSettle();
      expect(find.text('Name'), findsOneWidget);
      expect(find.text('Price'), findsOneWidget);
    });

    testWidgets('calls onSelected with ascending direction for new field',
        (tester) async {
      _Field? selectedField;
      SortDirection? selectedDirection;
      await tester.pumpWidget(
        _wrap(
          PrefabSortMenu<_Field>(
            options: options,
            onSelected: (f, d) {
              selectedField = f;
              selectedDirection = d;
            },
          ),
        ),
      );
      await tester.tap(find.byIcon(Icons.sort));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Name'));
      await tester.pumpAndSettle();
      expect(selectedField, _Field.name);
      expect(selectedDirection, SortDirection.ascending);
    });

    testWidgets('toggles direction when same field is selected again',
        (tester) async {
      SortDirection? selectedDirection;
      await tester.pumpWidget(
        _wrap(
          PrefabSortMenu<_Field>(
            options: options,
            currentValue: _Field.name,
            currentDirection: SortDirection.ascending,
            onSelected: (_, d) => selectedDirection = d,
          ),
        ),
      );
      await tester.tap(find.byIcon(Icons.sort));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Name'));
      await tester.pumpAndSettle();
      expect(selectedDirection, SortDirection.descending);
    });

    testWidgets('shows ascending arrow for the currently selected field',
        (tester) async {
      await tester.pumpWidget(
        _wrap(
          PrefabSortMenu<_Field>(
            options: options,
            currentValue: _Field.price,
            currentDirection: SortDirection.ascending,
            onSelected: (_, __) {},
          ),
        ),
      );
      await tester.tap(find.byIcon(Icons.sort));
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.arrow_upward), findsOneWidget);
    });

    testWidgets('shows descending arrow when direction is descending',
        (tester) async {
      await tester.pumpWidget(
        _wrap(
          PrefabSortMenu<_Field>(
            options: options,
            currentValue: _Field.price,
            currentDirection: SortDirection.descending,
            onSelected: (_, __) {},
          ),
        ),
      );
      await tester.tap(find.byIcon(Icons.sort));
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.arrow_downward), findsOneWidget);
    });
  });
}
