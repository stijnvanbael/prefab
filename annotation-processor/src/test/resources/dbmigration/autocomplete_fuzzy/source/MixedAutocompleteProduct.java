package dbmigration.autocomplete_fuzzy;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.core.annotations.rest.MatchStrategy;
import be.appify.prefab.core.annotations.rest.ScanMode;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record MixedAutocompleteProduct(
        @Id Reference<MixedAutocompleteProduct> id,
        @Version long version,
        @Autocomplete(matchStrategy = MatchStrategy.FUZZY) @NotNull String productName,
        @Autocomplete(matchStrategy = MatchStrategy.FUZZY, scanMode = ScanMode.CONTAINS) @NotNull String description,
        @Autocomplete(matchStrategy = MatchStrategy.IGNORE_CASE) @NotNull String category,
        @NotNull String sku
) {
    public MixedAutocompleteProduct(
            @NotNull String productName,
            @NotNull String description,
            @NotNull String category,
            @NotNull String sku
    ) {
        this(Reference.create(), 0L, productName, description, category, sku);
    }
}

