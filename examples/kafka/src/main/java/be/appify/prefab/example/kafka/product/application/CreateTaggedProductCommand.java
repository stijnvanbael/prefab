package be.appify.prefab.example.kafka.product.application;

import be.appify.prefab.example.kafka.product.ProductTag;
import java.util.List;

/**
 * Input for creating a product and applying its initial tags in the same use case.
 *
 * @param product the generated product creation request
 * @param tags the initial tags to apply
 */
public record CreateTaggedProductCommand(
        CreateProductRequest product,
        List<ProductTag> tags
) {
    /**
     * Copies the tag list so the command cannot be changed after construction.
     */
    public CreateTaggedProductCommand {
        tags = List.copyOf(tags);
    }
}
