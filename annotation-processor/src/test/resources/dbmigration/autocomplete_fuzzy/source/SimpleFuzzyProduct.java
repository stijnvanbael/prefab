package dbmigration.autocomplete_fuzzy;

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
public record SimpleFuzzyProduct(
        @Id Reference<SimpleFuzzyProduct> id,
        @Version long version,
        @Autocomplete(matchStrategy = MatchStrategy.FUZZY) @NotNull String name
) {
    public SimpleFuzzyProduct(@NotNull String name) {
        this(Reference.create(), 0L, name);
    }
}

