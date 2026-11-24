package rest.nestedvalid;

import jakarta.validation.constraints.NotBlank;

public record Address(@NotBlank String street, @NotBlank String city) {
}
