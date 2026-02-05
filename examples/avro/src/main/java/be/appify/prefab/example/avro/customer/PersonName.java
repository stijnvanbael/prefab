package be.appify.prefab.example.avro.customer;

import jakarta.validation.constraints.NotNull;

public record PersonName(
        @NotNull String firstName,
        @NotNull String lastName
) {
}
