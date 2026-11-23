package com.example.billing;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Invoice(
        @Id Reference<Invoice> id,
        @Version long version,
        @NotNull String number,
        @NotNull BigDecimal amount
) {
    @Create
    public Invoice(@NotNull String number, @NotNull BigDecimal amount) {
        this(Reference.create(), 0L, number, amount);
    }
}
