/// Annotations for Prefab Flutter code generation.
///
/// Annotate your model classes with [View], [Create], [Update], [Delete],
/// and [Api], plus field-level annotations ([ListColumn], [FormField],
/// [Parent]) to have Prefab Flutter generate a full-featured CRUD UI.
///
/// ## Minimal example
///
/// ```dart
/// import 'package:prefab_flutter_annotations/prefab_flutter_annotations.dart';
///
/// part 'product.g.dart';
///
/// @View(title: 'Products')
/// @Create()
/// @Update()
/// @Delete()
/// @Api(path: '/products')
/// class Product {
///   final String id;
///
///   @ListColumn(label: 'Name', sortable: true, searchable: true)
///   final String name;
///
///   @ListColumn(label: 'Price')
///   final double price;
///
///   const Product({required this.id, required this.name, required this.price});
/// }
/// ```
library prefab_flutter_annotations;

export 'src/annotations.dart';
