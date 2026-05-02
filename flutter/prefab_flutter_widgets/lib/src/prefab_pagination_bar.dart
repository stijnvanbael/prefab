import 'package:flutter/material.dart';

/// A notifier that tracks pagination state for use with [PrefabPaginationBar].
///
/// Expose this from a provider (e.g. Riverpod `StateProvider` or plain
/// `ValueNotifier`) and pass it to [PrefabPaginationBar].
class PaginationNotifier extends ValueNotifier<int> {
  final int totalPages;

  PaginationNotifier({required int initialPage, required this.totalPages})
      : super(initialPage);

  /// Zero-based index of the current page.
  int get currentPage => value;

  void goToPage(int page) {
    assert(page >= 0 && page < totalPages);
    value = page;
  }

  void previousPage() {
    if (value > 0) value--;
  }

  void nextPage() {
    if (value < totalPages - 1) value++;
  }
}

/// A row of page-navigation controls connected to a [PaginationNotifier].
///
/// Displays a previous-page button, a next-page button, and the current page /
/// total-pages label in between.
///
/// ```dart
/// PrefabPaginationBar(notifier: _paginationNotifier)
/// ```
class PrefabPaginationBar extends StatelessWidget {
  /// The notifier that drives this widget.  The bar rebuilds automatically
  /// whenever the current page changes.
  final PaginationNotifier notifier;

  const PrefabPaginationBar({super.key, required this.notifier});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<int>(
      valueListenable: notifier,
      builder: (context, currentPage, _) {
        final isFirst = currentPage == 0;
        final isLast = currentPage >= notifier.totalPages - 1;

        return Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            IconButton(
              icon: const Icon(Icons.chevron_left),
              tooltip: 'Previous page',
              onPressed: isFirst ? null : notifier.previousPage,
            ),
            Text(
              '${currentPage + 1} / ${notifier.totalPages}',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            IconButton(
              icon: const Icon(Icons.chevron_right),
              tooltip: 'Next page',
              onPressed: isLast ? null : notifier.nextPage,
            ),
          ],
        );
      },
    );
  }
}
