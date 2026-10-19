package dbmigration.autocomplete_fulltext;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.core.annotations.rest.MatchStrategy;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record FullTextProduct(
        @Id Reference<FullTextProduct> id,
        @Version long version,
        @Autocomplete(matchStrategy = MatchStrategy.FULL_TEXT) @NotNull String name
) {
    public FullTextProduct(@NotNull String name) {
        this(Reference.create(), 0L, name);
    }
}

