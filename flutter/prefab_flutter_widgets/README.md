# prefab_flutter_widgets

Runtime Flutter widgets used by Prefab-generated list screens, forms, and dialogs.

## Widgets

| Widget / Class | Purpose |
|---|---|
| `PrefabPage<T>` | Model that holds a page of items with pagination metadata |
| `PrefabSearchBar` | Search field with a clear button and `onChanged` callback |
| `PrefabPaginationBar` | Previous / next page controls driven by a `PaginationNotifier` |
| `PrefabDeleteDialog<T>` | Confirmation dialog before deleting an item |
| `PrefabSortMenu<T>` | Popup menu for selecting a sort column and direction |

## Usage

```dart
import 'package:prefab_flutter_widgets/prefab_flutter_widgets.dart';
```

### PrefabPage

```dart
final page = PrefabPage<Product>(
  items: products,
  totalItems: 100,
  pageSize: 20,
  currentPage: 0,
);
print(page.totalPages); // 5
```

### PrefabSearchBar

```dart
PrefabSearchBar(
  hintText: 'Search products…',
  onChanged: (query) => controller.search(query),
)
```

### PrefabPaginationBar

```dart
final notifier = PaginationNotifier(initialPage: 0, totalPages: 5);

PrefabPaginationBar(notifier: notifier)
```

### PrefabDeleteDialog

```dart
final confirmed = await showDialog<bool>(
  context: context,
  builder: (_) => PrefabDeleteDialog<Product>(
    item: product,
    itemLabel: product.name,
    onConfirm: (p) async => await api.delete(p.id),
  ),
);
```

### PrefabSortMenu

```dart
PrefabSortMenu<ProductSortField>(
  options: const [
    SortOption(label: 'Name',  value: ProductSortField.name),
    SortOption(label: 'Price', value: ProductSortField.price),
  ],
  currentValue: _sortField,
  currentDirection: _sortDirection,
  onSelected: (field, direction) => setState(() {
    _sortField = field;
    _sortDirection = direction;
  }),
)
```

## Running tests

```sh
flutter test
```
