package be.appify.prefab.example.sale;

import jakarta.validation.constraints.NotNull;

public record PersonName(
    @NotNull String firstName,
    @NotNull String lastName
) {
}
