import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:prefab_flutter_widgets/prefab_flutter_widgets.dart';

Widget _wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

void main() {
  group('PaginationNotifier', () {
    test('initial page is set correctly', () {
      final notifier =
          PaginationNotifier(initialPage: 0, totalPages: 5);
      expect(notifier.currentPage, 0);
    });

    test('goToPage updates current page', () {
      final notifier =
          PaginationNotifier(initialPage: 0, totalPages: 5);
      notifier.goToPage(3);
      expect(notifier.currentPage, 3);
    });

    test('nextPage increments current page', () {
      final notifier =
          PaginationNotifier(initialPage: 1, totalPages: 5);
      notifier.nextPage();
      expect(notifier.currentPage, 2);
    });

    test('nextPage does nothing on last page', () {
      final notifier =
          PaginationNotifier(initialPage: 4, totalPages: 5);
      notifier.nextPage();
      expect(notifier.currentPage, 4);
    });

    test('previousPage decrements current page', () {
      final notifier =
          PaginationNotifier(initialPage: 2, totalPages: 5);
      notifier.previousPage();
      expect(notifier.currentPage, 1);
    });

    test('previousPage does nothing on first page', () {
      final notifier =
          PaginationNotifier(initialPage: 0, totalPages: 5);
      notifier.previousPage();
      expect(notifier.currentPage, 0);
    });
  });

  group('PrefabPaginationBar', () {
    testWidgets('shows page label', (tester) async {
      final notifier =
          PaginationNotifier(initialPage: 0, totalPages: 5);
      await tester.pumpWidget(
        _wrap(PrefabPaginationBar(notifier: notifier)),
      );
      expect(find.text('1 / 5'), findsOneWidget);
    });

    testWidgets('previous button is disabled on first page', (tester) async {
      final notifier =
          PaginationNotifier(initialPage: 0, totalPages: 5);
      await tester.pumpWidget(
        _wrap(PrefabPaginationBar(notifier: notifier)),
      );
      final prevButton = tester.widget<IconButton>(
        find.widgetWithIcon(IconButton, Icons.chevron_left),
      );
      expect(prevButton.onPressed, isNull);
    });

    testWidgets('next button is disabled on last page', (tester) async {
      final notifier =
          PaginationNotifier(initialPage: 4, totalPages: 5);
      await tester.pumpWidget(
        _wrap(PrefabPaginationBar(notifier: notifier)),
      );
      final nextButton = tester.widget<IconButton>(
        find.widgetWithIcon(IconButton, Icons.chevron_right),
      );
      expect(nextButton.onPressed, isNull);
    });

    testWidgets('tapping next advances the page', (tester) async {
      final notifier =
          PaginationNotifier(initialPage: 0, totalPages: 5);
      await tester.pumpWidget(
        _wrap(PrefabPaginationBar(notifier: notifier)),
      );
      await tester.tap(find.byIcon(Icons.chevron_right));
      await tester.pump();
      expect(find.text('2 / 5'), findsOneWidget);
    });

    testWidgets('tapping previous goes back a page', (tester) async {
      final notifier =
          PaginationNotifier(initialPage: 2, totalPages: 5);
      await tester.pumpWidget(
        _wrap(PrefabPaginationBar(notifier: notifier)),
      );
      await tester.tap(find.byIcon(Icons.chevron_left));
      await tester.pump();
      expect(find.text('2 / 5'), findsOneWidget);
    });
  });
}
