package mother.childwithparent;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.GetById;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Organisation(
        @Id String id,
        @Version long version,
        String name) {

    @be.appify.prefab.core.annotations.rest.Create
    public Organisation(String name) {
        this(UUID.randomUUID().toString(), 0L, name);
    }
}

