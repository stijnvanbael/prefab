package rest.autocomplete;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.core.annotations.rest.MatchStrategy;
import be.appify.prefab.core.annotations.rest.ScanMode;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        @Autocomplete(scanMode = ScanMode.CONTAINS, matchStrategy = MatchStrategy.IGNORE_CASE) String name,
        @Autocomplete(path = "/brands/search") String brand
) {
}

