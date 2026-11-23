package com.example.ordering;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull @Email String customerEmail,
        @NotNull BigDecimal total,
        @NotNull OrderStatus status
) {
    @Create
    public Order(@NotNull @Email String customerEmail, @NotNull BigDecimal total) {
        this(Reference.create(), 0L, customerEmail, total, OrderStatus.OPEN);
    }

    @Update(path = "/ship", method = "POST")
    public Order ship() {
        return new Order(id, version, customerEmail, total, OrderStatus.SHIPPED);
    }
}
