package rest.override;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Widget(
        @Id String id,
        @Version long version,
        String name) {
    @Create
    public Widget(String name) {
        this(UUID.randomUUID().toString(), 0L, name);
    }
}
