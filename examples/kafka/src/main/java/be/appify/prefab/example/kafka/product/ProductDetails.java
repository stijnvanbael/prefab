package be.appify.prefab.example.kafka.product;

import be.appify.prefab.core.annotations.DbDocument;
import be.appify.prefab.core.annotations.Indexed;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@DbDocument
public record ProductDetails(
        @Nullable @Size(max = 1000) String description,
        @NotNull @Indexed String category
) {
}
