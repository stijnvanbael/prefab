import 'package:flutter/material.dart';

/// A column sort direction.
enum SortDirection { ascending, descending }

/// A sort option displayed in [PrefabSortMenu].
class SortOption<T> {
  /// Display label shown in the menu.
  final String label;

  /// The value passed to [PrefabSortMenu.onSelected] when this option is
  /// chosen.
  final T value;

  const SortOption({required this.label, required this.value});
}

/// A popup menu button that lets the user pick a sort column and direction.
///
/// [T] is the type of the sort field identifier (e.g. an enum or a `String`).
///
/// ```dart
/// PrefabSortMenu<ProductSortField>(
///   options: const [
///     SortOption(label: 'Name', value: ProductSortField.name),
///     SortOption(label: 'Price', value: ProductSortField.price),
///   ],
///   currentValue: _sortField,
///   currentDirection: _sortDirection,
///   onSelected: (field, direction) {
///     setState(() {
///       _sortField = field;
///       _sortDirection = direction;
///     });
///   },
/// )
/// ```
class PrefabSortMenu<T> extends StatelessWidget {
  /// The available sort options.
  final List<SortOption<T>> options;

  /// Currently active sort field.  The matching option is shown with a leading
  /// arrow icon.
  final T? currentValue;

  /// Current sort direction, used to render the directional arrow.
  final SortDirection currentDirection;

  /// Called when the user picks a sort option.  Receives the new field value
  /// and the toggled direction when the same field is re-selected.
  final void Function(T field, SortDirection direction) onSelected;

  /// Tooltip shown on the icon button.
  final String tooltip;

  const PrefabSortMenu({
    super.key,
    required this.options,
    required this.onSelected,
    this.currentValue,
    this.currentDirection = SortDirection.ascending,
    this.tooltip = 'Sort',
  });

  void _handleSelection(BuildContext context, T selectedValue) {
    final SortDirection newDirection;
    if (selectedValue == currentValue) {
      newDirection = currentDirection == SortDirection.ascending
          ? SortDirection.descending
          : SortDirection.ascending;
    } else {
      newDirection = SortDirection.ascending;
    }
    onSelected(selectedValue, newDirection);
  }

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<T>(
      tooltip: tooltip,
      icon: const Icon(Icons.sort),
      onSelected: (value) => _handleSelection(context, value),
      itemBuilder: (_) => options.map((option) {
        final isSelected = option.value == currentValue;
        return PopupMenuItem<T>(
          value: option.value,
          child: Row(
            children: [
              if (isSelected)
                Icon(
                  currentDirection == SortDirection.ascending
                      ? Icons.arrow_upward
                      : Icons.arrow_downward,
                  size: 16,
                )
              else
                const SizedBox(width: 16),
              const SizedBox(width: 8),
              Text(option.label),
            ],
          ),
        );
      }).toList(),
    );
  }
}
