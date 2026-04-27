package dbmigration.jsonbdocument;

import be.appify.prefab.core.annotations.DbDocument;
import be.appify.prefab.core.annotations.Indexed;
import jakarta.validation.constraints.Size;

@DbDocument
public record ProductDetails(
        @Size(max = 500) String description,
        @Indexed String category
) {
}
