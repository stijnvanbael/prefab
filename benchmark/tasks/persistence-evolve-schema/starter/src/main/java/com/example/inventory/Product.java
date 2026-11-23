package com.example.inventory;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotNull String name,
        @NotNull BigDecimal price
) {
    @Create
    public Product(@NotNull String name, @NotNull BigDecimal price) {
        this(Reference.create(), 0L, name, price);
    }
}
