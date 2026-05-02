import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:prefab_flutter_widgets/prefab_flutter_widgets.dart';

Widget _wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

void main() {
  group('PrefabSearchBar', () {
    testWidgets('renders with default hint text', (tester) async {
      await tester.pumpWidget(
        _wrap(PrefabSearchBar(onChanged: (_) {})),
      );
      expect(find.text('Search…'), findsOneWidget);
    });

    testWidgets('renders with custom hint text', (tester) async {
      await tester.pumpWidget(
        _wrap(PrefabSearchBar(hintText: 'Find items', onChanged: (_) {})),
      );
      expect(find.text('Find items'), findsOneWidget);
    });

    testWidgets('calls onChanged when text is typed', (tester) async {
      String captured = '';
      await tester.pumpWidget(
        _wrap(PrefabSearchBar(onChanged: (v) => captured = v)),
      );
      await tester.enterText(find.byType(TextField), 'hello');
      expect(captured, 'hello');
    });

    testWidgets('shows clear button when text is non-empty', (tester) async {
      await tester.pumpWidget(
        _wrap(PrefabSearchBar(onChanged: (_) {})),
      );
      await tester.enterText(find.byType(TextField), 'abc');
      await tester.pump();
      expect(find.byIcon(Icons.clear), findsOneWidget);
    });

    testWidgets('clear button clears text and notifies onChanged',
        (tester) async {
      String captured = 'initial';
      await tester.pumpWidget(
        _wrap(PrefabSearchBar(onChanged: (v) => captured = v)),
      );
      await tester.enterText(find.byType(TextField), 'query');
      await tester.pump();
      await tester.tap(find.byIcon(Icons.clear));
      await tester.pump();
      expect(captured, '');
      expect(find.byIcon(Icons.clear), findsNothing);
    });
  });
}
