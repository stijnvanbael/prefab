import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:prefab_flutter_widgets/prefab_flutter_widgets.dart';

Widget _wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

void main() {
  group('PrefabDeleteDialog', () {
    testWidgets('shows default title and message', (tester) async {
      await tester.pumpWidget(
        _wrap(
          Builder(
            builder: (context) => TextButton(
              onPressed: () => showDialog<bool>(
                context: context,
                builder: (_) => const PrefabDeleteDialog<String>(
                  item: 'Widget A',
                  itemLabel: 'Widget A',
                ),
              ),
              child: const Text('Open'),
            ),
          ),
        ),
      );
      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();
      expect(find.text('Delete Widget A?'), findsOneWidget);
      expect(find.textContaining('"Widget A"'), findsOneWidget);
    });

    testWidgets('Cancel closes dialog without confirming', (tester) async {
      bool? result;
      await tester.pumpWidget(
        _wrap(
          Builder(
            builder: (context) => TextButton(
              onPressed: () async {
                result = await showDialog<bool>(
                  context: context,
                  builder: (_) => const PrefabDeleteDialog<String>(
                    item: 'x',
                    itemLabel: 'Item X',
                  ),
                );
              },
              child: const Text('Open'),
            ),
          ),
        ),
      );
      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Cancel'));
      await tester.pumpAndSettle();
      expect(result, false);
    });

    testWidgets('Delete button closes dialog with true', (tester) async {
      bool? result;
      await tester.pumpWidget(
        _wrap(
          Builder(
            builder: (context) => TextButton(
              onPressed: () async {
                result = await showDialog<bool>(
                  context: context,
                  builder: (_) => const PrefabDeleteDialog<String>(
                    item: 'x',
                    itemLabel: 'Item X',
                  ),
                );
              },
              child: const Text('Open'),
            ),
          ),
        ),
      );
      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Delete'));
      await tester.pumpAndSettle();
      expect(result, true);
    });

    testWidgets('onConfirm callback is invoked with item', (tester) async {
      String? confirmed;
      await tester.pumpWidget(
        _wrap(
          Builder(
            builder: (context) => TextButton(
              onPressed: () => showDialog<bool>(
                context: context,
                builder: (_) => PrefabDeleteDialog<String>(
                  item: 'Widget B',
                  itemLabel: 'Widget B',
                  onConfirm: (item) async => confirmed = item,
                ),
              ),
              child: const Text('Open'),
            ),
          ),
        ),
      );
      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Delete'));
      await tester.pumpAndSettle();
      expect(confirmed, 'Widget B');
    });
  });
}
