package com.example.inventory;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbDefaultValue;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotNull String name,
        @NotNull BigDecimal price,
        @Nullable String description,
        @DbDefaultValue("0") int stock
) {
    @Create
    public Product(@NotNull String name, @NotNull BigDecimal price, @Nullable String description, int stock) {
        this(Reference.create(), 0L, name, price, description, stock);
    }
}
