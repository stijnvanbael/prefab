package be.appify.prefab.example.sale;

import jakarta.validation.constraints.NotNull;

public record Address(
    @NotNull String street,
    String number,
    @NotNull String postalCode,
    @NotNull String city,
    @NotNull String country
) {
}
