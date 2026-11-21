package com.example.retail;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Sale(
        @Id Reference<Sale> id,
        @Version long version,
        @NotNull Reference<CashRegister> cashRegister,
        double amount,
        boolean paid
) {
    @Create
    public Sale(@NotNull Reference<CashRegister> cashRegister, double amount) {
        this(Reference.create(), 0L, cashRegister, amount, false);
    }

    @Update(path = "/pay", method = "POST")
    public Sale pay() {
        return new Sale(id, version, cashRegister, amount, true);
    }
}
