package rest.autocomplete;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Autocomplete;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        @Autocomplete(ignoreCase = true) String name,
        @Autocomplete String brand
) {
}

