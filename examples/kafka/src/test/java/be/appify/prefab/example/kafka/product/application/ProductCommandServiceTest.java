package be.appify.prefab.example.kafka.product.application;

import be.appify.prefab.example.kafka.product.Product;
import be.appify.prefab.example.kafka.product.ProductDetails;
import be.appify.prefab.example.kafka.product.ProductTag;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductCommandServiceTest {
    private final ProductService productService = mock(ProductService.class);
    private final ProductCommandService commandService = new ProductCommandService(productService);

    @Test
    void createTaggedProduct_createsThenAppliesTagsInOrder() {
        var command = command();
        when(productService.create(command.product())).thenReturn("product-1");
        when(productService.addTag("product-1", new ProductAddTagRequest(command.tags().getFirst())))
                .thenReturn(Optional.of(product()));
        when(productService.addTag("product-1", new ProductAddTagRequest(command.tags().get(1))))
                .thenReturn(Optional.of(product()));

        var productId = commandService.createTaggedProduct(command);

        assertThat(productId).isEqualTo("product-1");
        InOrder order = inOrder(productService);
        order.verify(productService).create(command.product());
        order.verify(productService).addTag("product-1", new ProductAddTagRequest(command.tags().getFirst()));
        order.verify(productService).addTag("product-1", new ProductAddTagRequest(command.tags().get(1)));
    }

    @Test
    void createTaggedProduct_failsWhenAnUpdateCannotFindTheCreatedProduct() {
        var command = command();
        when(productService.create(command.product())).thenReturn("product-1");
        when(productService.addTag("product-1", new ProductAddTagRequest(command.tags().getFirst())))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.createTaggedProduct(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Product disappeared before its initial tags were applied");
    }

    private static CreateTaggedProductCommand command() {
        return new CreateTaggedProductCommand(
                new CreateProductRequest("Camera", new ProductDetails("Mirrorless camera", "Electronics")),
                List.of(new ProductTag("color", "black"), new ProductTag("mount", "X")));
    }

    private static Product product() {
        return new Product(
                be.appify.prefab.core.service.Reference.create(),
                0L,
                "Camera",
                new ProductDetails("Mirrorless camera", "Electronics"),
                List.of());
    }
}
