/// A page of items returned from a paginated API.
///
/// [T] is the item type held in [items].  [totalItems] and [pageSize] drive the
/// [PrefabPaginationBar] widget.
class PrefabPage<T> {
  /// The items on the current page.
  final List<T> items;

  /// Total number of items across all pages.
  final int totalItems;

  /// Maximum number of items per page.
  final int pageSize;

  /// Zero-based index of the current page.
  final int currentPage;

  const PrefabPage({
    required this.items,
    required this.totalItems,
    required this.pageSize,
    required this.currentPage,
  });

  /// The total number of pages, calculated from [totalItems] and [pageSize].
  int get totalPages => pageSize > 0 ? (totalItems / pageSize).ceil() : 0;

  /// Returns `true` when there is no content.
  bool get isEmpty => items.isEmpty;

  /// Returns a copy of this page with selected fields replaced.
  PrefabPage<T> copyWith({
    List<T>? items,
    int? totalItems,
    int? pageSize,
    int? currentPage,
  }) {
    return PrefabPage<T>(
      items: items ?? this.items,
      totalItems: totalItems ?? this.totalItems,
      pageSize: pageSize ?? this.pageSize,
      currentPage: currentPage ?? this.currentPage,
    );
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PrefabPage<T> &&
          items == other.items &&
          totalItems == other.totalItems &&
          pageSize == other.pageSize &&
          currentPage == other.currentPage;

  @override
  int get hashCode =>
      Object.hash(items, totalItems, pageSize, currentPage);

  @override
  String toString() =>
      'PrefabPage(items: ${items.length}, totalItems: $totalItems, '
      'pageSize: $pageSize, currentPage: $currentPage)';
}
