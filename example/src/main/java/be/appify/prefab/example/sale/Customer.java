package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import jakarta.validation.constraints.NotNull;

@Aggregate
@GetById
@DbMigration(version = 2)
public record Customer(
    @NotNull PersonName name,
    @NotNull Address address,
    @NotNull String email
) {
    @Create
    public Customer {
    }
}
