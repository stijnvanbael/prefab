import 'package:flutter/material.dart';

/// A search bar that notifies listeners whenever the user changes the query.
///
/// Wrap it in a [Padding] or place it directly in a column / app-bar.
///
/// ```dart
/// PrefabSearchBar(
///   hintText: 'Search products…',
///   onChanged: (query) => ref.read(productListProvider.notifier).search(query),
/// )
/// ```
class PrefabSearchBar extends StatefulWidget {
  /// Placeholder text shown when the field is empty.
  final String hintText;

  /// Called every time the search text changes.
  final ValueChanged<String> onChanged;

  /// Optional icon shown at the start of the field.
  final Icon? prefixIcon;

  const PrefabSearchBar({
    super.key,
    this.hintText = 'Search…',
    required this.onChanged,
    this.prefixIcon,
  });

  @override
  State<PrefabSearchBar> createState() => _PrefabSearchBarState();
}

class _PrefabSearchBarState extends State<PrefabSearchBar> {
  final TextEditingController _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onClear() {
    _controller.clear();
    widget.onChanged('');
  }

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: _controller,
      decoration: InputDecoration(
        hintText: widget.hintText,
        prefixIcon: widget.prefixIcon ?? const Icon(Icons.search),
        suffixIcon: ValueListenableBuilder<TextEditingValue>(
          valueListenable: _controller,
          builder: (_, value, __) => value.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear),
                  tooltip: 'Clear search',
                  onPressed: _onClear,
                )
              : const SizedBox.shrink(),
        ),
        border: const OutlineInputBorder(),
      ),
      onChanged: widget.onChanged,
    );
  }
}
