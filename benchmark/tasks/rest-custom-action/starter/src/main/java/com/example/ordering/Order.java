package com.example.ordering;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull String customer,
        @NotNull OrderStatus status
) {
    @Create
    public Order(@NotNull String customer) {
        this(Reference.create(), 0L, customer, OrderStatus.OPEN);
    }
}
