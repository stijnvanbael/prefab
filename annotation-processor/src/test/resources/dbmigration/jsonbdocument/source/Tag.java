package dbmigration.jsonbdocument;

import be.appify.prefab.core.annotations.DbDocument;
import jakarta.validation.constraints.Size;

@DbDocument
public record Tag(
        @Size(max = 50) String name,
        @Size(max = 100) String value
) {
}
