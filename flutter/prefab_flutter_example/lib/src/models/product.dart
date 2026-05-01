import 'package:json_annotation/json_annotation.dart';
import 'package:prefab_flutter_annotations/prefab_flutter_annotations.dart';

part 'product.g.dart';         // json_serializable generated
part 'product.prefab.g.dart';  // Prefab Flutter generated

/// A product in the catalogue.
///
/// Annotate with [@PrefabView], [@PrefabCreate], [@PrefabUpdate],
/// [@PrefabDelete], and [@PrefabApi] to generate a complete CRUD UI.
/// Prefab Flutter generates:
///
/// - [ProductListScreen]  — paginated, searchable, sortable list
/// - [ProductDetailScreen] — read-only detail view
/// - [ProductCreateScreen] — create form
/// - [ProductEditScreen]  — edit form with pre-populated fields
/// - [ProductListNotifier] — Riverpod [AsyncNotifier] provider
/// - [ProductApiClient]   — dio REST client
/// - [ProductListRoute], [ProductDetailRoute], ... — typed go_router routes
@PrefabView(title: 'Products')
@PrefabCreate(title: 'New Product', submitLabel: 'Create')
@PrefabUpdate(title: 'Edit Product', submitLabel: 'Save')
@PrefabDelete(confirmMessage: 'Remove this product permanently?')
@PrefabApi(path: '/products')
@JsonSerializable()
class Product {
  final String id;

  @ListColumn(label: 'Name', sortable: true, searchable: true)
  @FormField(label: 'Name', hint: 'Enter product name', validators: [Validator.required])
  final String name;

  @ListColumn(label: 'Price (€)', sortable: true, flex: 1)
  @FormField(label: 'Price', hint: '0.00', validators: [Validator.required, Validator.positiveNumber])
  final double price;

  @ListColumn(label: 'Category', sortable: false, searchable: true)
  @FormField(label: 'Category')
  final String category;

  @Hidden()
  final String? description;

  const Product({
    required this.id,
    required this.name,
    required this.price,
    required this.category,
    this.description,
  });

  factory Product.fromJson(Map<String, dynamic> json) => _$ProductFromJson(json);
  Map<String, dynamic> toJson() => _$ProductToJson(this);
}
