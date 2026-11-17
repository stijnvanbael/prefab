package be.appify.prefab.example.kafka.product.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Example application adapter composing generated product operations into one command.
 */
@Service
public class ProductCommandService {
    private final ProductService productService;

    /**
     * Creates the command adapter.
     *
     * @param productService the generated product service
     */
    public ProductCommandService(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Creates a product and saves each initial tag within the caller's PostgreSQL transaction.
     *
     * @param command the product and tags to create
     * @return the generated product identifier
     * @throws IllegalStateException if the just-created product cannot be updated
     */
    @Transactional
    public String createTaggedProduct(CreateTaggedProductCommand command) {
        var productId = productService.create(command.product());
        for (var tag : command.tags()) {
            if (productService.addTag(productId, new ProductAddTagRequest(tag)).isEmpty()) {
                throw new IllegalStateException("Product disappeared before its initial tags were applied");
            }
        }
        return productId;
    }
}
