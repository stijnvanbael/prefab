package be.appify.prefab.example.kafka.product;

import be.appify.prefab.core.annotations.DbDocument;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@DbDocument
public record ProductTag(
        @NotNull @Size(max = 50) String name,
        @NotNull @Size(max = 100) String value
) {
}
