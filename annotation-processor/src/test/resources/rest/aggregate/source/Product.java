package rest.aggregate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        String name) {

    @Create
    public Product(String name) {
        this(UUID.randomUUID().toString(), 0L, name);
    }
}

