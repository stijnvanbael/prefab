import 'package:flutter/material.dart';

/// A confirmation dialog shown before deleting an item of type [T].
///
/// Returns `true` from [showDialog] when the user confirms, `false` (or `null`)
/// when the user cancels.
///
/// ```dart
/// final confirmed = await showDialog<bool>(
///   context: context,
///   builder: (_) => PrefabDeleteDialog<Product>(
///     item: product,
///     itemLabel: product.name,
///   ),
/// );
/// if (confirmed == true) await ref.read(productListProvider.notifier).delete(product.id);
/// ```
class PrefabDeleteDialog<T> extends StatelessWidget {
  /// The item that will be deleted (used in [onConfirm]).
  final T item;

  /// Human-readable name/label for [item] shown inside the dialog.
  final String itemLabel;

  /// Optional title override.  Defaults to `'Delete $itemLabel?'`.
  final String? title;

  /// Optional body text override.
  final String? message;

  /// Called when the user taps the delete button.  If `null` the dialog simply
  /// closes with `true`.
  final Future<void> Function(T item)? onConfirm;

  const PrefabDeleteDialog({
    super.key,
    required this.item,
    required this.itemLabel,
    this.title,
    this.message,
    this.onConfirm,
  });

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(title ?? 'Delete $itemLabel?'),
      content: Text(
        message ??
            'Are you sure you want to delete "$itemLabel"? '
                'This action cannot be undone.',
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text('Cancel'),
        ),
        TextButton(
          style: TextButton.styleFrom(
            foregroundColor: Theme.of(context).colorScheme.error,
          ),
          onPressed: () async {
            await onConfirm?.call(item);
            if (context.mounted) Navigator.of(context).pop(true);
          },
          child: const Text('Delete'),
        ),
      ],
    );
  }
}
