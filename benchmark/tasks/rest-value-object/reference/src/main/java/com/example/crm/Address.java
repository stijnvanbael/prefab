package com.example.crm;

import jakarta.validation.constraints.NotNull;

public record Address(@NotNull String street, @NotNull String city, @NotNull String postalCode, @NotNull String country) {
}
